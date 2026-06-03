package org.ubiquia.core.communication.controller.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.dto.Flow;

/**
 * Proxy controller that routes communication-service requests to the Flow Service
 * <em>flow</em> endpoints.
 *
 * <p>
 * Mounted at {@code /ubiquia/core/communication-service/flow-service/flow}, this façade
 * forwards requests to the downstream Flow Service {@code /ubiquia/core/flow-service/flow}
 * endpoints. All proxy mechanics are inherited from {@link AbstractUbiquiaDaoControllerProxy}.
 * </p>
 */
@RestController
@RequestMapping("/ubiquia/core/communication-service/flow-service/flow")
public class FlowControllerProxy extends AbstractUbiquiaDaoControllerProxy<Flow> {

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Override
    public String getUrlHelper() {
        return this.flowServiceConfig.getUrl()
            + ":"
            + this.flowServiceConfig.getPort().toString()
            + "/ubiquia/core/flow-service/flow";
    }
}
