package org.ubiquia.core.communication.controller.flow;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.core.communication.service.manager.flow.ComponentProxyManager;

@RestController
@RequestMapping("/ubiquia/communication-service/component-reverse-proxy")
public class ComponentReverseProxyController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ComponentProxyManager componentProxyManager;
    private final FlowServiceConfig flowServiceConfig;
    private final Set<String> hopByHopHeaders;

    public ComponentReverseProxyController(
        final ComponentProxyManager componentProxyManager,
        final FlowServiceConfig flowServiceConfig
    ) {
        this.componentProxyManager = componentProxyManager;
        this.flowServiceConfig = flowServiceConfig;
        this.hopByHopHeaders = new HashSet<>(Arrays.asList(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailer", "transfer-encoding", "upgrade",
            "host", "content-length", "content-encoding", "accept-encoding"
        ));
    }

    @GetMapping("/get-proxied-urls")
    public List<String> getProxiedUrls() {
        var result = this.componentProxyManager.getRegisteredEndpoints();
        return result;
    }

    @RequestMapping(
        value = "/{componentName}/**",
        method = {
            RequestMethod.GET, RequestMethod.HEAD,
            RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.PATCH, RequestMethod.DELETE
        }
    )
    public void proxyToComponent(
        @PathVariable final String componentName,
        final HttpServletRequest request,
        final HttpServletResponse response
    ) throws IOException {

        var endpoint = this.componentProxyManager.getRegisteredEndpointFor(componentName);
        if (endpoint == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No component registered with name: " + componentName);
        }

        // -------- derive proxied prefix + tail --------
        var pathWithin = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        var bestPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        var tail = new AntPathMatcher().extractPathWithinPattern(bestPattern, pathWithin);
        var remainder = (tail == null) ? "" : tail;

        String proxiedPrefix;
        if (pathWithin != null) {
            proxiedPrefix = pathWithin.substring(0, pathWithin.length() - ((tail == null) ? 0 : tail.length()));
        } else {
            var uri = request.getRequestURI();
            proxiedPrefix = uri.substring(0, Math.max(uri.lastIndexOf('/') + 1, 0));
        }
        if (!proxiedPrefix.endsWith("/")) {
            proxiedPrefix += "/";
        }
        this.logger.debug("proxiedPrefix={}", proxiedPrefix);

        var staticAsset = this.isStaticAsset(remainder);

        // -------- choose upstream base --------
        URI base;
        if (endpoint.isAbsolute()) {
            base = staticAsset ? this.hostRoot(endpoint) : endpoint;
        } else {
            base = this.buildServiceBase(this.flowServiceConfig.getUrl(), this.flowServiceConfig.getPort(), endpoint);
        }

        var target = this.buildTargetUri(base, remainder, request.getQueryString());
        this.logger.debug("Reverse proxying {} -> {}", request.getMethod(), target);
        response.setHeader("X-Ubiquia-Target", target.toString());

        // -------- open upstream connection --------
        var conn = (HttpURLConnection) new URL(target.toString()).openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod(request.getMethod());
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(60_000);

        // Copy headers (skip hop-by-hop / managed)
        var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            var h = headerNames.nextElement();
            var lower = (h == null) ? "" : h.toLowerCase(Locale.ROOT);
            var shouldCopy = h != null && !this.hopByHopHeaders.contains(lower);
            if (Boolean.TRUE.equals(shouldCopy)) {
                var vals = request.getHeaders(h);
                while (vals.hasMoreElements()) {
                    conn.addRequestProperty(h, vals.nextElement());
                }
            }
        }
        // Force identity so we can safely rewrite HTML/CSS (no gzip)
        conn.setRequestProperty("Accept-Encoding", "identity");

        // Body (if any)
        if (Boolean.TRUE.equals(this.hasRequestBody(request.getMethod(), request))) {
            conn.setDoOutput(true);
            try (var in = request.getInputStream()) {
                IOUtils.copy(in, conn.getOutputStream());
                conn.getOutputStream().flush();
            }
        }

        var status = this.safeResponseCode(conn);
        response.setStatus(status);
        var contentType = Optional.ofNullable(conn.getContentType()).orElse("");
        var isHtml = contentType.toLowerCase(Locale.ROOT).contains("text/html");
        var isCss = contentType.toLowerCase(Locale.ROOT).contains("text/css");
        var isHead = "HEAD".equalsIgnoreCase(request.getMethod());

        // Copy response headers (skip hop-by-hop; rewrite Location if root-absolute)
        var upstreamHeaders = conn.getHeaderFields();
        if (upstreamHeaders != null) {
            for (var e : upstreamHeaders.entrySet()) {
                var name = e.getKey();
                var values = e.getValue();
                var nameIsNull = (name == null);
                var lname = nameIsNull ? "" : name.toLowerCase(Locale.ROOT);
                var skip = nameIsNull || this.hopByHopHeaders.contains(lname);

                if (!skip && values != null) {
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
        }

        // Self-healing fallback for assets that came back as HTML (index fallback)
        var canRetryFromRoot = status < 400
            && Boolean.TRUE.equals(staticAsset)
            && isHtml
            && endpoint.isAbsolute()
            && ("GET".equalsIgnoreCase(request.getMethod()) || "HEAD".equalsIgnoreCase(request.getMethod()));

        if (Boolean.TRUE.equals(canRetryFromRoot)) {
            var alt = this.buildTargetUri(this.hostRoot(endpoint), remainder, request.getQueryString());
            this.logger.warn("Static asset returned HTML; retrying via host root: {}", alt);
            response.setHeader("X-Ubiquia-Target-Retry", alt.toString());

            conn.disconnect();
            conn = (HttpURLConnection) new URL(alt.toString()).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(60_000);
            conn.setRequestProperty("Accept-Encoding", "identity");

            status = this.safeResponseCode(conn);
            response.setStatus(status);
            contentType = Optional.ofNullable(conn.getContentType()).orElse("");
            isHtml = contentType.toLowerCase(Locale.ROOT).contains("text/html");
            isCss = contentType.toLowerCase(Locale.ROOT).contains("text/css");
        }

        // -------- stream body --------
        try (var out = response.getOutputStream()) {
            if (status >= 400) {
                var err = conn.getErrorStream();
                var canCopy = err != null && !Boolean.TRUE.equals(isHead);
                if (Boolean.TRUE.equals(canCopy)) {
                    IOUtils.copy(err, out);
                    out.flush();
                }
            } else if (!Boolean.TRUE.equals(isHead)) {
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

    // ----------------- URL building -----------------

    private URI buildServiceBase(final String hostUrl, final Integer port, final URI endpointPath) {
        var baseHost = hostUrl;
        var lower = (hostUrl == null) ? "" : hostUrl.toLowerCase(Locale.ROOT);
        if (hostUrl == null || !(lower.startsWith("http://") || lower.startsWith("https://"))) {
            baseHost = "http://" + (hostUrl == null ? "localhost" : hostUrl);
        }
        var b = UriComponentsBuilder
            .fromHttpUrl(this.stripTrailingSlash(baseHost))
            .port(port)
            .path("/")
            .path(this.stripLeadingSlash(Objects.toString(endpointPath.getPath(), "")));
        if (endpointPath.getQuery() != null && !endpointPath.getQuery().isEmpty()) {
            b.query(endpointPath.getQuery());
        }
        var result = b.build(true).toUri();
        return result;
    }

    private URI buildTargetUri(final URI base, final String remainder, final String rawQuery) {
        var b = UriComponentsBuilder.fromUri(base);
        if (remainder != null && !remainder.isEmpty()) {
            b.path("/").path(this.stripLeadingSlash(remainder));
        }
        var baseQuery = base.getQuery();
        if (rawQuery != null && !rawQuery.isEmpty()) {
            var combined = (baseQuery != null && !baseQuery.isEmpty()) ? baseQuery + "&" + rawQuery : rawQuery;
            b.query(combined);
        } else if (baseQuery != null && !baseQuery.isEmpty()) {
            b.query(baseQuery);
        }
        var result = b.build(true).toUri();
        return result;
    }

    // ----------------- misc helpers -----------------

    private Boolean hasRequestBody(final String method, final HttpServletRequest request) {
        var result = Boolean.FALSE;
        if (method != null) {
            switch (method) {
                case "POST":
                case "PUT":
                case "PATCH":
                    result = (request.getContentLengthLong() > 0) || (request.getHeader("Transfer-Encoding") != null);
                    break;
                default:
                    result = Boolean.FALSE;
                    break;
            }
        }
        return result;
    }

    private Integer safeResponseCode(final HttpURLConnection c) {
        Integer result;
        try {
            result = c.getResponseCode();
        } catch (IOException e) {
            result = HttpStatus.BAD_GATEWAY.value();
        }
        return result;
    }

    private String stripLeadingSlash(final String s) {
        var result = (s == null) ? "" : s.replaceFirst("^/+", "");
        return result;
    }

    private String stripTrailingSlash(final String s) {
        var result = (s == null) ? "" : s.replaceFirst("/+$", "");
        return result;
    }

    private Charset charsetFrom(final String contentType) {
        Charset result;
        try {
            if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("charset=")) {
                var parts = contentType.split("(?i)charset=");
                var v = parts[1].trim().replaceAll("[;\\s].*$", "");
                result = Charset.forName(v);
            } else {
                result = StandardCharsets.UTF_8;
            }
        } catch (Exception ignore) {
            result = StandardCharsets.UTF_8;
        }
        return result;
    }

    private String contentTypeWithCharset(final String ct, final String charset) {
        String result;
        if (ct == null || ct.isBlank()) {
            result = "text/html; charset=" + charset;
        } else {
            result = ct.replaceAll("(?i);\\s*charset=[^;]+", "") + "; charset=" + charset;
        }
        return result;
    }

    private String rewriteLocationIfNeeded(final String location, final String proxiedPrefix) {
        String result;
        if (location == null || location.isBlank()) {
            result = location;
        } else if (location.startsWith("/")) {
            result = proxiedPrefix + this.stripLeadingSlash(location);
        } else {
            result = location;
        }
        return result;
    }

    // ----------------- HTML/CSS rewriting -----------------

    private String setOrInjectBaseHref(final String html, final String prefix) {
        var replaced = html.replaceFirst(
            "(?is)<base\\s+[^>]*href\\s*=\\s*(['\"]).*?\\1[^>]*>",
            "<base href=\"" + Matcher.quoteReplacement(prefix) + "\">"
        );
        String result;
        if (!replaced.equals(html)) {
            result = replaced;
        } else {
            result = html.replaceFirst(
                "(?is)<head(\\s[^>]*)?>",
                "<head$1><base href=\"" + Matcher.quoteReplacement(prefix) + "\">"
            );
        }
        return result;
    }

    private String rewriteAssetUrlsInHtml(final String html, final String prefix) {
        var p = Matcher.quoteReplacement(prefix);
        var out = html;

        out = out.replaceAll("(?is)(<(?:script|img)\\b[^>]*\\bsrc\\s*=\\s*\")/", "$1" + p);
        out = out.replaceAll("(?is)(<link\\b[^>]*\\bhref\\s*=\\s*\")/", "$1" + p);

        out = out.replaceAll(
            "(?is)(<(?:script|img)\\b[^>]*\\bsrc\\s*=\\s*\")(?!(?:[a-zA-Z][a-zA-Z0-9+.-]*:|//|data:|blob:|/))",
            "$1" + p
        );
        out = out.replaceAll(
            "(?is)(<link\\b[^>]*\\bhref\\s*=\\s*\")(?!(?:[a-zA-Z][a-zA-Z0-9+.-]*:|//|data:|blob:|/))",
            "$1" + p
        );

        out = out.replaceAll("(?is)(<link\\b[^>]*rel\\s*=\\s*\"modulepreload\"[^>]*\\bhref\\s*=\\s*\")/", "$1" + p);

        var result = out;
        return result;
    }

    private String rewriteRootAbsoluteUrlsInCss(final String css, final String prefix) {
        var p = Matcher.quoteReplacement(prefix);
        var result = css
            .replaceAll("(?is)url\\(\\s*/", "url(" + p)
            .replaceAll("(?is)@import\\s+([\"'])/", "@import $1" + p);
        return result;
    }

    // ----------------- asset routing helpers -----------------

    private Boolean isStaticAsset(final String path) {
        Boolean result;
        if (path == null) {
            result = Boolean.FALSE;
        } else {
            var p = path.toLowerCase(Locale.ROOT);
            result = p.matches(".*\\.(js|mjs|css|map|ico|png|jpg|jpeg|gif|svg|webp|woff2?|ttf|eot)$")
                || p.startsWith("assets/");
        }
        return result;
    }

    private URI hostRoot(final URI u) {
        var result = UriComponentsBuilder.newInstance()
            .scheme(u.getScheme())
            .host(u.getHost())
            .port(u.getPort())
            .build(true).toUri();
        return result;
    }
}
