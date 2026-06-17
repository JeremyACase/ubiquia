package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.dto.Component;

/**
 * Proxy controller for Flow Service component endpoints.
 *
 * <p>Forwards requests to the downstream Flow Service component resource. Proxy mechanics
 * are inherited from {@link AbstractUbiquiaDaoControllerProxy}.</p>
 */
@RestController
@RequestMapping("/ubiquia/core/communication-service/flow-service/component")
public class ComponentControllerProxy extends AbstractUbiquiaDaoControllerProxy<Component> {

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Override
    public String getUrlHelper() {
        return this.flowServiceConfig.getBaseUrl() + "/ubiquia/core/flow-service/component";
    }
}
