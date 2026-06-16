package org.ubiquia.core.communication.service.proxy;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.io.IOUtils;

/**
 * Builder for {@link HttpURLConnection} instances used by reverse-proxy controllers.
 *
 * <p>Always sets {@code instanceFollowRedirects = false} and {@code Accept-Encoding: identity}
 * so that response bodies can be read and optionally rewritten without decompression.</p>
 */
public class HttpProxyConnectionBuilder {

    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 60_000;

    private final URI target;
    private String method = "GET";
    private HttpServletRequest headerSource;
    private Set<String> headersToSkip = Set.of();
    private HttpServletRequest bodySource;

    /** Creates a builder targeting the given URI. */
    public HttpProxyConnectionBuilder(final URI target) {
        this.target = target;
    }

    /** Sets the HTTP method; defaults to {@code GET}. */
    public HttpProxyConnectionBuilder method(final String method) {
        this.method = method;
        return this;
    }

    /**
     * Copies non-hop-by-hop headers from {@code request} to the connection.
     *
     * @param request  source of the headers to forward
     * @param skipList lower-cased header names to omit
     */
    public HttpProxyConnectionBuilder copyHeadersFrom(
        final HttpServletRequest request,
        final Set<String> skipList) {
        this.headerSource = request;
        this.headersToSkip = skipList;
        return this;
    }

    /**
     * Streams the request body for POST, PUT, and PATCH requests when a body is present.
     *
     * @param request source of the request body
     */
    public HttpProxyConnectionBuilder withBodyFrom(final HttpServletRequest request) {
        this.bodySource = request;
        return this;
    }

    /** Builds and returns a configured {@link HttpURLConnection} ready for use. */
    public HttpURLConnection build() throws IOException {
        var conn = (HttpURLConnection) new URL(this.target.toString()).openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod(this.method);
        conn.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(DEFAULT_READ_TIMEOUT_MS);

        if (this.headerSource != null) {
            var headerNames = this.headerSource.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                var h = headerNames.nextElement();
                var lower = h == null ? "" : h.toLowerCase(Locale.ROOT);
                if (h != null && !this.headersToSkip.contains(lower)) {
                    var vals = this.headerSource.getHeaders(h);
                    while (vals.hasMoreElements()) {
                        conn.addRequestProperty(h, vals.nextElement());
                    }
                }
            }
        }
        conn.setRequestProperty("Accept-Encoding", "identity");

        if (this.bodySource != null) {
            var m = this.bodySource.getMethod();
            var hasBody = ("POST".equals(m) || "PUT".equals(m) || "PATCH".equals(m))
                && (this.bodySource.getContentLengthLong() > 0
                    || this.bodySource.getHeader("Transfer-Encoding") != null);
            if (hasBody) {
                conn.setDoOutput(true);
                try (var in = this.bodySource.getInputStream()) {
                    IOUtils.copy(in, conn.getOutputStream());
                    conn.getOutputStream().flush();
                }
            }
        }

        return conn;
    }
}
