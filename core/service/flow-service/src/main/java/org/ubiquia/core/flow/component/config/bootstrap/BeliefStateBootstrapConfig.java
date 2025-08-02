package org.ubiquia.core.flow.component.config.bootstrap;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.ubiquia.common.model.ubiquia.embeddable.BeliefStateGeneration;

/**
 * Configuration component that maintains what graphs we're configured to deploy
 * after registration.
 */
@Component
@ConditionalOnProperty(
    value = "ubiquia.agent.flow-service.bootstrap.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@ConfigurationProperties("ubiquia.agent.flow-service.bootstrap.belief-states")
public class BeliefStateBootstrapConfig {

    private Boolean enabled;

    private List<BeliefStateGeneration> deployments;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<BeliefStateGeneration> getDeployments() {
        return deployments;
    }

    public void setDeployments(List<BeliefStateGeneration> deployments) {
        this.deployments = deployments;
    }
}
