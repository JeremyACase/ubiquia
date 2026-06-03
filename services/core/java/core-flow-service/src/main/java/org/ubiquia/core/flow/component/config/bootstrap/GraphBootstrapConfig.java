package org.ubiquia.core.flow.component.config.bootstrap;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;

/**
 * Configuration component that maintains what graphs we're configured to deploy
 * after bootstrap.
 */
@Component
@ConditionalOnProperty(
    value = "ubiquia.agent.flow-service.bootstrap.graph.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@ConfigurationProperties("ubiquia.agent.flow-service.bootstrap.graph")
@Validated
public class GraphBootstrapConfig {

    private Boolean enabled;

    private List<GraphDeployment> deployments;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<GraphDeployment> getDeployments() {
        return deployments;
    }

    public void setDeployments(List<GraphDeployment> deployments) {
        this.deployments = deployments;
    }
}
