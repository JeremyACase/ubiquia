package org.ubiquia.core.communication.controller.util.dashboard;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import org.ubiquia.core.communication.config.DashboardServiceConfig;
import org.ubiquia.core.communication.controller.AbstractReverseProxyController;

/**
 * Reverse proxy for the Ubiquia Dashboard UI.
 *
 * <p>Forwards all requests under {@code /ubiquia/core/communication-service/dashboard/**}
 * to the dashboard nginx pod, with HTML/CSS rewriting so assets resolve
 * through the proxy prefix.</p>
 */
@RestController
@RequestMapping("/ubiquia/core/communication-service/dashboard")
public class DashboardProxyController extends AbstractReverseProxyController {

    @Autowired
    private DashboardServiceConfig dashboardServiceConfig;

    /** Proxies all requests to the upstream dashboard service. */
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
        this.executeProxy(request, response, Map.of());
    }

    @Override
    protected UpstreamResolution resolveUpstream(
        final HttpServletRequest request,
        final String remainder,
        final boolean isStaticAsset,
        final Map<String, String> pathVars) {

        var base = UriComponentsBuilder
            .fromHttpUrl(this.dashboardServiceConfig.getBaseUrl())
            .build(true).toUri();

        return UpstreamResolution.of(base);
    }
}
