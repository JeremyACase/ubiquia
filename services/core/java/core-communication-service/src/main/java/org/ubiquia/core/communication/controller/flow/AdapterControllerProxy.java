package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.dto.Graph;

/**
 * Proxy controller that routes communication-service requests to the Flow Service
 * <em>adapter</em> endpoints.
 *
 * <p>
 * Mounted at {@code /ubiquia/communication-service/flow-service/adapter}, this façade forwards
 * requests to the downstream Flow Service base {@code /ubiquia/flow-service/adapter}. All proxy
 * mechanics (propagating method, headers, query params, and body; reactive I/O; response handling)
 * are inherited from {@link AbstractUbiquiaDaoControllerProxy}.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Provide the downstream base URL for adapter endpoints via {@link #getUrlHelper()}.</li>
 *   <li>Leverage inherited handlers (e.g., {@code /query/params} and
 *       {@code proxyToPostEndpoint(...)} in the base class) to reach the Flow Service.</li>
 * </ul>
 *
 * <p>Downstream host/port are sourced from {@link FlowServiceConfig}.</p>
 */
@RestController
@RequestMapping("/ubiquia/communication-service/flow-service/adapter")
public class AdapterControllerProxy extends AbstractUbiquiaDaoControllerProxy<Graph> {

    /** Flow Service host/port configuration used to build the downstream base URL. */
    @Autowired
    private FlowServiceConfig flowServiceConfig;

    /**
     * Builds the base URL for the Flow Service <em>adapter</em> endpoints.
     *
     * <p>Example format: {@code http://&lt;host&gt;:&lt;port&gt;/ubiquia/flow-service/adapter}</p>
     *
     * @return fully qualified base URL used by inherited proxy methods
     */
    @Override
    public String getUrlHelper() {
        var url = this.flowServiceConfig.getUrl()
            + ":"
            + this.flowServiceConfig.getPort().toString()
            + "/ubiquia/flow-service/adapter";
        return url;
    }
}
