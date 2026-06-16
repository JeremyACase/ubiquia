package org.ubiquia.core.communication.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.core.communication.service.proxy.HttpProxyConnectionBuilder;
import org.ubiquia.core.communication.service.proxy.ProxyResponseRewriter;

/**
 * Template-Method base for servlet-based reverse proxy controllers.
 *
 * <p>Handles the complete proxy lifecycle: path derivation, upstream connection, header
 * forwarding, optional HTML/CSS rewriting, static-asset index-fallback retry, and response
 * streaming. Subclasses provide upstream target resolution via
 * {@link #resolveUpstream(HttpServletRequest, String, boolean, Map)}.</p>
 */
@RestController
public abstract class AbstractReverseProxyController {

    private static final Logger logger =
        LoggerFactory.getLogger(AbstractReverseProxyController.class);

    protected final Set<String> hopByHopHeaders;

    @Autowired
    private ProxyResponseRewriter proxyResponseRewriter;

    /** Initializes the set of hop-by-hop headers that must not be forwarded upstream. */
    protected AbstractReverseProxyController() {
        this.hopByHopHeaders = new HashSet<>(Arrays.asList(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailer", "transfer-encoding", "upgrade",
            "host", "content-length", "content-encoding", "accept-encoding"
        ));
    }

    /**
     * Carries the resolved upstream base URI and whether an index-fallback retry is permitted.
     */
    protected record UpstreamResolution(URI base, boolean allowsStaticAssetRetry) {

        /** Factory: creates a resolution that allows static-asset retry. */
        public static UpstreamResolution of(URI base) {
            return new UpstreamResolution(base, true);
        }

        /** Factory: creates a resolution with an explicit retry policy. */
        public static UpstreamResolution of(URI base, boolean allowsRetry) {
            return new UpstreamResolution(base, allowsRetry);
        }
    }

    /**
     * Template method: resolves the upstream base URI for an incoming request.
     *
     * @param request       the incoming request
     * @param remainder     the tail path after the controller's prefix
     * @param isStaticAsset whether {@code remainder} resembles a static asset
     * @param pathVars      Spring path variables captured by the subclass handler
     * @return upstream resolution including the base URI and retry policy
     */
    protected abstract UpstreamResolution resolveUpstream(
        HttpServletRequest request,
        String remainder,
        boolean isStaticAsset,
        Map<String, String> pathVars);

    /**
     * Hook: override to disable HTML/CSS body rewriting. Defaults to {@code true}.
     */
    protected boolean rewritesHtmlAndCss() {
        return true;
    }

    /**
     * Executes the full reverse-proxy lifecycle for the current request.
     *
     * @param request  incoming client request
     * @param response outgoing response
     * @param pathVars Spring path variables captured by the subclass handler
     */
    protected void executeProxy(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Map<String, String> pathVars) throws IOException {

        var pathWithin = (String) request
            .getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        var bestPattern = (String) request
            .getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        var tail = new AntPathMatcher().extractPathWithinPattern(bestPattern, pathWithin);
        var remainder = tail == null ? "" : tail;

        String proxiedPrefix;
        if (pathWithin != null) {
            proxiedPrefix = pathWithin
                .substring(0, pathWithin.length() - (tail == null ? 0 : tail.length()));
        } else {
            var uri = request.getRequestURI();
            proxiedPrefix = uri.substring(0, Math.max(uri.lastIndexOf('/') + 1, 0));
        }
        if (!proxiedPrefix.endsWith("/")) {
            proxiedPrefix += "/";
        }

        var staticAsset = this.isStaticAsset(remainder);
        var upstream = this.resolveUpstream(request, remainder, staticAsset, pathVars);
        var target = this.buildTargetUri(upstream.base(), remainder, request.getQueryString());

        logger.debug("Proxy {} -> {}", request.getMethod(), target);
        response.setHeader("X-Ubiquia-Target", target.toString());

        var conn = new HttpProxyConnectionBuilder(target)
            .method(request.getMethod())
            .copyHeadersFrom(request, this.hopByHopHeaders)
            .withBodyFrom(request)
            .build();

        var status = this.safeResponseCode(conn);
        response.setStatus(status);

        var contentType = Optional.ofNullable(conn.getContentType()).orElse("");
        var isHtml = contentType.toLowerCase(Locale.ROOT).contains("text/html");
        var isCss = contentType.toLowerCase(Locale.ROOT).contains("text/css");
        var isHead = "HEAD".equalsIgnoreCase(request.getMethod());

        this.copyResponseHeaders(conn, response, proxiedPrefix);

        var canRetry = status < 400
            && staticAsset
            && isHtml
            && upstream.allowsStaticAssetRetry()
            && ("GET".equalsIgnoreCase(request.getMethod()) || isHead);

        if (canRetry) {
            var alt = this.buildTargetUri(upstream.base(), remainder, request.getQueryString());
            logger.warn("Static asset returned HTML; retrying: {}", alt);
            response.setHeader("X-Ubiquia-Target-Retry", alt.toString());

            conn.disconnect();
            conn = new HttpProxyConnectionBuilder(alt).method("GET").build();

            status = this.safeResponseCode(conn);
            response.setStatus(status);
            contentType = Optional.ofNullable(conn.getContentType()).orElse("");
            isHtml = contentType.toLowerCase(Locale.ROOT).contains("text/html");
            isCss = contentType.toLowerCase(Locale.ROOT).contains("text/css");
        }

        this.streamResponse(
            conn, response, status, isHtml, isCss, isHead, contentType, proxiedPrefix);
    }

    private void copyResponseHeaders(
        final HttpURLConnection conn,
        final HttpServletResponse response,
        final String proxiedPrefix) {

        var upstreamHeaders = conn.getHeaderFields();
        if (upstreamHeaders == null) {
            return;
        }
        for (var e : upstreamHeaders.entrySet()) {
            var name = e.getKey();
            var values = e.getValue();
            if (name == null || values == null) {
                continue;
            }
            var lname = name.toLowerCase(Locale.ROOT);
            if (this.hopByHopHeaders.contains(lname)) {
                continue;
            }
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

    private void streamResponse(
        final HttpURLConnection conn,
        final HttpServletResponse response,
        final int status,
        final boolean isHtml,
        final boolean isCss,
        final boolean isHead,
        final String contentType,
        final String proxiedPrefix) throws IOException {

        try (var out = response.getOutputStream()) {
            if (status >= 400) {
                var err = conn.getErrorStream();
                if (err != null && !isHead) {
                    IOUtils.copy(err, out);
                    out.flush();
                }
            } else if (!isHead) {
                if (this.rewritesHtmlAndCss() && (isHtml || isCss)) {
                    var cs = this.proxyResponseRewriter.charsetFrom(contentType);
                    var raw = IOUtils.toByteArray(conn.getInputStream());
                    var body = new String(raw, cs);
                    if (isHtml) {
                        body = this.proxyResponseRewriter.rewriteHtml(body, proxiedPrefix);
                    } else {
                        body = this.proxyResponseRewriter.rewriteCss(body, proxiedPrefix);
                    }
                    var outBytes = body.getBytes(StandardCharsets.UTF_8);
                    response.setHeader("Content-Type",
                        this.proxyResponseRewriter.contentTypeWithCharset(contentType, "utf-8"));
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

    /**
     * Builds the full upstream target URI from a base URI, path remainder, and query string.
     *
     * @param base      upstream base URI (may already carry a path and query)
     * @param remainder tail path to append after the base
     * @param rawQuery  raw query string from the client request, or {@code null}
     * @return composed target URI
     */
    protected URI buildTargetUri(final URI base, final String remainder, final String rawQuery) {
        var b = UriComponentsBuilder.fromUri(base);
        if (remainder != null && !remainder.isEmpty()) {
            b.path("/").path(this.stripLeadingSlash(remainder));
        }
        var baseQuery = base.getQuery();
        if (rawQuery != null && !rawQuery.isEmpty()) {
            b.query(Objects.nonNull(baseQuery) && !baseQuery.isEmpty()
                ? baseQuery + "&" + rawQuery : rawQuery);
        } else if (Objects.nonNull(baseQuery) && !baseQuery.isEmpty()) {
            b.query(baseQuery);
        }
        return b.build(true).toUri();
    }

    /** Strips a leading slash (or slashes) from {@code s}; returns {@code ""} for null input. */
    protected String stripLeadingSlash(final String s) {
        return s == null ? "" : s.replaceFirst("^/+", "");
    }

    /** Strips a trailing slash (or slashes) from {@code s}; returns {@code ""} for null input. */
    protected String stripTrailingSlash(final String s) {
        return s == null ? "" : s.replaceFirst("/+$", "");
    }

    private int safeResponseCode(final HttpURLConnection c) {
        try {
            return c.getResponseCode();
        } catch (IOException e) {
            return HttpStatus.BAD_GATEWAY.value();
        }
    }

    private String rewriteLocationIfNeeded(final String location, final String proxiedPrefix) {
        if (location == null || location.isBlank()) {
            return location;
        }
        if (location.startsWith("/")) {
            return proxiedPrefix + this.stripLeadingSlash(location);
        }
        return location;
    }

    private boolean isStaticAsset(final String path) {
        if (path == null) {
            return false;
        }
        var p = path.toLowerCase(Locale.ROOT);
        return p.matches(".*\\.(js|mjs|css|map|ico|png|jpg|jpeg|gif|svg|webp|woff2?|ttf|eot)$")
            || p.startsWith("assets/");
    }
}
