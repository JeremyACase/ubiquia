package org.ubiquia.core.communication.controller.flow;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.core.communication.controller.AbstractReverseProxyController;
import org.ubiquia.core.communication.service.manager.flow.ComponentProxyManager;

/**
 * Reverse proxy for component endpoints with HTML/CSS URL rewriting.
 *
 * <p>Forwards requests under
 * {@code /ubiquia/core-communication-service/{graph}/component/{component}/**}
 * to the upstream component URI registered with {@link ComponentProxyManager}.
 * Absolute endpoints serve static assets from the host root; relative endpoints
 * are resolved via {@link FlowServiceConfig}.</p>
 */
@RestController
@RequestMapping("/ubiquia/core-communication-service")
public class DeployedComponentProxyController extends AbstractReverseProxyController {

    @Autowired
    private ComponentProxyManager componentProxyManager;

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    /** Returns the list of upstream component URLs currently registered for proxying. */
    @GetMapping("/component/get-proxied-urls")
    public List<String> getProxiedUrls() {
        return this.componentProxyManager.getRegisteredEndpoints();
    }

    /** Proxies all HTTP methods to the registered upstream component endpoint. */
    @RequestMapping(
        value = "/{graph}/component/{component}/**",
        method = {
            RequestMethod.GET, RequestMethod.HEAD,
            RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.PATCH, RequestMethod.DELETE
        }
    )
    public void proxyToComponent(
        @PathVariable final String graph,
        @PathVariable final String component,
        final HttpServletRequest request,
        final HttpServletResponse response
    ) throws IOException {
        this.executeProxy(request, response, Map.of("graph", graph, "component", component));
    }

    @Override
    protected UpstreamResolution resolveUpstream(
        final HttpServletRequest request,
        final String remainder,
        final boolean isStaticAsset,
        final Map<String, String> pathVars) {

        var component = pathVars.get("component");
        var endpoint = this.componentProxyManager.getRegisteredEndpointFor(component);
        if (Objects.isNull(endpoint)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No component registered with name: " + component);
        }

        if (endpoint.isAbsolute()) {
            var base = isStaticAsset ? this.hostRoot(endpoint) : endpoint;
            return UpstreamResolution.of(base, true);
        }

        var base = this.buildServiceBase(endpoint);
        return UpstreamResolution.of(base, false);
    }

    private URI buildServiceBase(final URI endpointPath) {
        var b = UriComponentsBuilder
            .fromHttpUrl(this.stripTrailingSlash(this.flowServiceConfig.getBaseUrl()))
            .path("/")
            .path(this.stripLeadingSlash(Objects.toString(endpointPath.getPath(), "")));
        if (Objects.nonNull(endpointPath.getQuery()) && !endpointPath.getQuery().isEmpty()) {
            b.query(endpointPath.getQuery());
        }
        return b.build(true).toUri();
    }

    private URI hostRoot(final URI u) {
        return UriComponentsBuilder.newInstance()
            .scheme(u.getScheme())
            .host(u.getHost())
            .port(u.getPort())
            .build(true).toUri();
    }
}
