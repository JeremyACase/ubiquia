package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.dto.FlowEvent;

/**
 * Proxy controller that routes communication-service requests to the Flow Service
 * <em>event</em> endpoints.
 *
 * <p>
 * This controller mounts under
 * {@code /ubiquia/communication-service/flow-service/graph} as a façade, but
 * configures its downstream base URL to {@code /ubiquia/flow-service/event}.
 * All proxy mechanics (forwarding method/headers/query/body, reactive I/O, and
 * deserialization) are inherited from
 * {@link AbstractUbiquiaDaoControllerProxy}.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Provide the downstream base URL for event endpoints via {@link #getUrlHelper()}.</li>
 *   <li>Leverage inherited handlers (e.g., {@code /query/params} and
 *       {@code proxyToPostEndpoint(...)} in the base class) to reach the Flow Service.</li>
 * </ul>
 *
 * <p>
 * Downstream host/port are sourced from {@link FlowServiceConfig}.
 * </p>
 */
@RestController
@RequestMapping("/ubiquia/communication-service/flow-service/event")
public class EventControllerProxy extends AbstractUbiquiaDaoControllerProxy<FlowEvent> {

    /**
     * Flow Service host/port configuration used to build the downstream base URL.
     */
    @Autowired
    private FlowServiceConfig flowServiceConfig;

    /**
     * Builds the base URL for the Flow Service <em>event</em> endpoints.
     *
     * <p>Example format: {@code http://<host>:<port>/ubiquia/flow-service/event}</p>
     *
     * @return fully qualified base URL used by inherited proxy methods
     */
    @Override
    public String getUrlHelper() {
        var url = this.flowServiceConfig.getUrl()
            + ":"
            + this.flowServiceConfig.getPort().toString()
            + "/ubiquia/flow-service/event";
        return url;
    }
}
