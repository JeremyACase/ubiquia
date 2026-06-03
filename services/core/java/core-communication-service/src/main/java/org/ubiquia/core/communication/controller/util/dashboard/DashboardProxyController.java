package org.ubiquia.core.communication.controller.util.dashboard;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.core.communication.config.DashboardServiceConfig;

/**
 * Reverse proxy for the Ubiquia Dashboard UI.
 *
 * <p>Requests to {@code /ubiquia/core/communication-service/dashboard/**} are forwarded to the
 * dashboard nginx pod. HTML responses receive an injected {@code <base href>} and asset URL
 * rewriting so all static resources resolve correctly through the proxy prefix. CSS root-absolute
 * references are similarly rewritten. Static assets that return HTML trigger an index-fallback
 * retry from the upstream root (SPA support).</p>
 */
@RestController
@RequestMapping("/ubiquia/core/communication-service/dashboard")
public class DashboardProxyController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardProxyController.class);

    private final Set<String> hopByHopHeaders;

    @Autowired
    private DashboardServiceConfig dashboardServiceConfig;

    public DashboardProxyController() {
        this.hopByHopHeaders = new HashSet<>(Arrays.asList(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailer", "transfer-encoding", "upgrade",
            "host", "content-length", "content-encoding", "accept-encoding"
        ));
    }

    /**
     * Proxies all HTTP methods to the dashboard upstream.
     *
     * @param request  incoming client request
     * @param response outgoing response to populate
     * @throws IOException on I/O errors while streaming request/response bodies
     */
    @RequestMapping(
        value = {"", "/", "/**"},
        method = {
            RequestMethod.GET, RequestMethod.HEAD,
            RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.PATCH, RequestMethod.DELETE
        }
    )
    public void proxyToDashboard(
        final HttpServletRequest request,
        final HttpServletResponse response
    ) throws IOException {

        // Derive proxied prefix and tail path
        var pathWithin = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        var bestPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        var tail = new AntPathMatcher().extractPathWithinPattern(bestPattern, pathWithin);
        var remainder = tail == null ? "" : tail;

        String proxiedPrefix;
        if (pathWithin != null) {
            proxiedPrefix = pathWithin.substring(0,
                pathWithin.length() - (tail == null ? 0 : tail.length()));
        } else {
            var uri = request.getRequestURI();
            proxiedPrefix = uri.substring(0, Math.max(uri.lastIndexOf('/') + 1, 0));
        }
        if (!proxiedPrefix.endsWith("/")) {
            proxiedPrefix += "/";
        }

        // Build upstream base and target URI
        var upstreamBase = UriComponentsBuilder
            .fromHttpUrl(this.dashboardServiceConfig.getUrl())
            .port(this.dashboardServiceConfig.getPort())
            .build(true).toUri();

        var target = this.buildTargetUri(upstreamBase, remainder, request.getQueryString());
        logger.debug("Dashboard proxy {} -> {}", request.getMethod(), target);
        response.setHeader("X-Ubiquia-Target", target.toString());

        // Open upstream connection
        var conn = (HttpURLConnection) new URL(target.toString()).openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod(request.getMethod());
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(60_000);

        // Forward request headers, skipping hop-by-hop
        var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            var h = headerNames.nextElement();
            var lower = h == null ? "" : h.toLowerCase(Locale.ROOT);
            if (h != null && !this.hopByHopHeaders.contains(lower)) {
                var vals = request.getHeaders(h);
                while (vals.hasMoreElements()) {
                    conn.addRequestProperty(h, vals.nextElement());
                }
            }
        }
        // Force identity encoding so HTML/CSS bodies can be rewritten without decompression
        conn.setRequestProperty("Accept-Encoding", "identity");

        // Forward request body for mutating methods
        var method = request.getMethod();
        var hasBody = ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))
            && (request.getContentLengthLong() > 0 || request.getHeader("Transfer-Encoding") != null);
        if (hasBody) {
            conn.setDoOutput(true);
            try (var in = request.getInputStream()) {
                IOUtils.copy(in, conn.getOutputStream());
                conn.getOutputStream().flush();
            }
        }

        int status;
        try {
            status = conn.getResponseCode();
        } catch (IOException e) {
            status = HttpStatus.BAD_GATEWAY.value();
        }
        response.setStatus(status);

        var contentType = Optional.ofNullable(conn.getContentType()).orElse("");
        var isHtml = contentType.toLowerCase(Locale.ROOT).contains("text/html");
        var isCss = contentType.toLowerCase(Locale.ROOT).contains("text/css");
        var isHead = "HEAD".equalsIgnoreCase(method);
        var isGet = "GET".equalsIgnoreCase(method);

        // Forward response headers, skipping hop-by-hop; rewrite root-absolute Location
        var upstreamHeaders = conn.getHeaderFields();
        if (upstreamHeaders != null) {
            for (var e : upstreamHeaders.entrySet()) {
                var name = e.getKey();
                var values = e.getValue();
                if (name == null || values == null) continue;
                var lname = name.toLowerCase(Locale.ROOT);
                if (this.hopByHopHeaders.contains(lname)) continue;
                if ("location".equals(lname)) {
                    for (var v : values) {
                        response.addHeader(name, this.rewriteLocationIfNeeded(v, proxiedPrefix));
                    }
                } else {
                    for (var v : values) {
                        response.addHeader(name, v);
                    }
                }
            }
        }

        // Static-asset index-fallback retry: if a .js/.css/etc. returns HTML, try from upstream root
        var isStaticAsset = this.isStaticAsset(remainder);
        var canRetry = status < 400 && isStaticAsset && isHtml && (isGet || isHead);
        if (canRetry) {
            var alt = this.buildTargetUri(upstreamBase, remainder, request.getQueryString());
            logger.warn("Static asset returned HTML; retrying from upstream root: {}", alt);
            response.setHeader("X-Ubiquia-Target-Retry", alt.toString());

            conn.disconnect();
            conn = (HttpURLConnection) new URL(alt.toString()).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(60_000);
            conn.setRequestProperty("Accept-Encoding", "identity");

            try {
                status = conn.getResponseCode();
            } catch (IOException e) {
                status = HttpStatus.BAD_GATEWAY.value();
            }
            response.setStatus(status);
            contentType = Optional.ofNullable(conn.getContentType()).orElse("");
            isHtml = contentType.toLowerCase(Locale.ROOT).contains("text/html");
            isCss = contentType.toLowerCase(Locale.ROOT).contains("text/css");
        }

        // Stream response body, rewriting HTML/CSS as needed
        try (var out = response.getOutputStream()) {
            if (status >= 400) {
                var err = conn.getErrorStream();
                if (err != null && !isHead) {
                    IOUtils.copy(err, out);
                    out.flush();
                }
            } else if (!isHead) {
                if (isHtml || isCss) {
                    var cs = this.charsetFrom(contentType);
                    var raw = IOUtils.toByteArray(conn.getInputStream());
                    var body = new String(raw, cs);

                    if (isHtml) {
                        body = this.setOrInjectBaseHref(body, proxiedPrefix);
                        body = this.rewriteAssetUrlsInHtml(body, proxiedPrefix);
                    } else {
                        body = this.rewriteRootAbsoluteUrlsInCss(body, proxiedPrefix);
                    }

                    var outBytes = body.getBytes(StandardCharsets.UTF_8);
                    response.setHeader("Content-Type", this.contentTypeWithCharset(contentType, "utf-8"));
                    response.setContentLength(outBytes.length);
                    out.write(outBytes);
                    out.flush();
                } else {
                    try (var upstream = conn.getInputStream()) {
                        IOUtils.copy(upstream, out);
                        out.flush();
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // URI helpers
    // -------------------------------------------------------------------------

    private URI buildTargetUri(final URI base, final String remainder, final String rawQuery) {
        var b = UriComponentsBuilder.fromUri(base);
        if (remainder != null && !remainder.isEmpty()) {
            b.path("/").path(remainder.startsWith("/") ? remainder.replaceFirst("^/+", "") : remainder);
        }
        var baseQuery = base.getQuery();
        if (rawQuery != null && !rawQuery.isEmpty()) {
            b.query(baseQuery != null && !baseQuery.isEmpty() ? baseQuery + "&" + rawQuery : rawQuery);
        } else if (baseQuery != null && !baseQuery.isEmpty()) {
            b.query(baseQuery);
        }
        return b.build(true).toUri();
    }

    private String rewriteLocationIfNeeded(final String location, final String proxiedPrefix) {
        if (location == null || location.isBlank()) return location;
        if (location.startsWith("/")) return proxiedPrefix + location.replaceFirst("^/+", "");
        return location;
    }

    private boolean isStaticAsset(final String path) {
        if (path == null) return false;
        var p = path.toLowerCase(Locale.ROOT);
        return p.matches(".*\\.(js|mjs|css|map|ico|png|jpg|jpeg|gif|svg|webp|woff2?|ttf|eot)$")
            || p.startsWith("assets/");
    }

    // -------------------------------------------------------------------------
    // HTML / CSS rewriting
    // -------------------------------------------------------------------------

    private String setOrInjectBaseHref(final String html, final String prefix) {
        var replaced = html.replaceFirst(
            "(?is)<base\\s+[^>]*href\\s*=\\s*(['\"]).*?\\1[^>]*>",
            "<base href=\"" + Matcher.quoteReplacement(prefix) + "\">"
        );
        if (!replaced.equals(html)) return replaced;
        return html.replaceFirst(
            "(?is)<head(\\s[^>]*)?>",
            "<head$1><base href=\"" + Matcher.quoteReplacement(prefix) + "\">"
        );
    }

    private String rewriteAssetUrlsInHtml(final String html, final String prefix) {
        var p = Matcher.quoteReplacement(prefix);
        var out = html;
        out = out.replaceAll("(?is)(<(?:script|img)\\b[^>]*\\bsrc\\s*=\\s*\")/", "$1" + p);
        out = out.replaceAll("(?is)(<link\\b[^>]*\\bhref\\s*=\\s*\")/", "$1" + p);
        out = out.replaceAll(
            "(?is)(<(?:script|img)\\b[^>]*\\bsrc\\s*=\\s*\")(?!(?:[a-zA-Z][a-zA-Z0-9+.-]*:|//|data:|blob:|/))",
            "$1" + p);
        out = out.replaceAll(
            "(?is)(<link\\b[^>]*\\bhref\\s*=\\s*\")(?!(?:[a-zA-Z][a-zA-Z0-9+.-]*:|//|data:|blob:|/))",
            "$1" + p);
        out = out.replaceAll(
            "(?is)(<link\\b[^>]*rel\\s*=\\s*\"modulepreload\"[^>]*\\bhref\\s*=\\s*\")/",
            "$1" + p);
        return out;
    }

    private String rewriteRootAbsoluteUrlsInCss(final String css, final String prefix) {
        var p = Matcher.quoteReplacement(prefix);
        return css
            .replaceAll("(?is)url\\(\\s*/", "url(" + p)
            .replaceAll("(?is)@import\\s+([\"'])/", "@import $1" + p);
    }

    // -------------------------------------------------------------------------
    // Charset helpers
    // -------------------------------------------------------------------------

    private Charset charsetFrom(final String contentType) {
        try {
            if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("charset=")) {
                var parts = contentType.split("(?i)charset=");
                var v = parts[1].trim().replaceAll("[;\\s].*$", "");
                return Charset.forName(v);
            }
        } catch (Exception ignored) {
            // fall through to default
        }
        return StandardCharsets.UTF_8;
    }

    private String contentTypeWithCharset(final String ct, final String charset) {
        if (ct == null || ct.isBlank()) return "text/html; charset=" + charset;
        return ct.replaceAll("(?i);\\s*charset=[^;]+", "") + "; charset=" + charset;
    }
}
