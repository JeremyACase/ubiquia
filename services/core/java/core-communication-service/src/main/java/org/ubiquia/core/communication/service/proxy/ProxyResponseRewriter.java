package org.ubiquia.core.communication.service.proxy;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import org.springframework.stereotype.Service;

/**
 * Rewrites HTML and CSS response bodies so asset URLs and base-href references
 * resolve correctly when served through a reverse-proxy prefix.
 */
@Service
public class ProxyResponseRewriter {

    /**
     * Rewrites an HTML body: injects/overrides {@code <base href>} and rewrites
     * root-absolute and relative asset URLs in {@code src} / {@code href} attributes.
     *
     * @param html          raw HTML body
     * @param proxiedPrefix the proxy-mounted prefix (e.g. {@code /ubiquia/.../dashboard/})
     * @return rewritten HTML
     */
    public String rewriteHtml(final String html, final String proxiedPrefix) {
        var out = this.setOrInjectBaseHref(html, proxiedPrefix);
        out = this.rewriteAssetUrlsInHtml(out, proxiedPrefix);
        return out;
    }

    /**
     * Rewrites a CSS body: rewrites root-absolute {@code url(/...)} and
     * {@code @import "/..."} references to go through the proxy prefix.
     *
     * @param css           raw CSS body
     * @param proxiedPrefix the proxy-mounted prefix
     * @return rewritten CSS
     */
    public String rewriteCss(final String css, final String proxiedPrefix) {
        return this.rewriteRootAbsoluteUrlsInCss(css, proxiedPrefix);
    }

    /**
     * Parses the charset from a {@code Content-Type} header value, defaulting to UTF-8.
     *
     * @param contentType value of the {@code Content-Type} header
     * @return resolved charset
     */
    public Charset charsetFrom(final String contentType) {
        try {
            if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("charset=")) {
                var parts = contentType.split("(?i)charset=");
                var v = parts[1].trim().replaceAll("[;\\s].*$", "");
                return Charset.forName(v);
            }
        } catch (Exception ignored) {
            // fall through to UTF-8
        }
        return StandardCharsets.UTF_8;
    }

    /**
     * Returns a {@code Content-Type} value with the charset parameter set to {@code charset},
     * replacing any existing charset declaration.
     *
     * @param contentType original content-type (may be null)
     * @param charset     charset token to inject (e.g. {@code utf-8})
     * @return content-type string with the charset parameter set
     */
    public String contentTypeWithCharset(final String contentType, final String charset) {
        if (contentType == null || contentType.isBlank()) {
            return "text/html; charset=" + charset;
        }
        return contentType.replaceAll("(?i);\\s*charset=[^;]+", "") + "; charset=" + charset;
    }

    private String setOrInjectBaseHref(final String html, final String prefix) {
        var replaced = html.replaceFirst(
            "(?is)<base\\s+[^>]*href\\s*=\\s*(['\"]).*?\\1[^>]*>",
            "<base href=\"" + Matcher.quoteReplacement(prefix) + "\">"
        );
        if (!replaced.equals(html)) {
            return replaced;
        }
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
            "(?is)(<(?:script|img)\\b[^>]*\\bsrc\\s*=\\s*\")"
                + "(?!(?:[a-zA-Z][a-zA-Z0-9+.-]*:|//|data:|blob:|/))",
            "$1" + p);
        out = out.replaceAll(
            "(?is)(<link\\b[^>]*\\bhref\\s*=\\s*\")"
                + "(?!(?:[a-zA-Z][a-zA-Z0-9+.-]*:|//|data:|blob:|/))",
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
}
