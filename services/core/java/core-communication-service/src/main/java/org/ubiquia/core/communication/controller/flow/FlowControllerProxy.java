package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.dto.Flow;

/**
 * Proxy controller for Flow Service flow endpoints.
 *
 * <p>Forwards requests to the downstream Flow Service flow resource. Proxy mechanics
 * are inherited from {@link AbstractUbiquiaDaoControllerProxy}.</p>
 */
@RestController
@RequestMapping("/ubiquia/core/communication-service/flow-service/flow")
public class FlowControllerProxy extends AbstractUbiquiaDaoControllerProxy<Flow> {

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Override
    public String getUrlHelper() {
        return this.flowServiceConfig.getBaseUrl() + "/ubiquia/core/flow-service/flow";
    }
}
