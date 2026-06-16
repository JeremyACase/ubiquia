package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.dto.Graph;

/**
 * Proxy controller for Flow Service node endpoints.
 *
 * <p>Forwards requests to the downstream Flow Service node resource. Proxy mechanics
 * are inherited from {@link AbstractUbiquiaDaoControllerProxy}.</p>
 */
@RestController
@RequestMapping("/ubiquia/core/communication-service/flow-service/node")
public class NodeControllerProxy extends AbstractUbiquiaDaoControllerProxy<Graph> {

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Override
    public String getUrlHelper() {
        return this.flowServiceConfig.getBaseUrl() + "/ubiquia/core/flow-service/node";
    }
}
