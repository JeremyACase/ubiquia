package org.ubiquia.core.flow.config;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.ubiquia.core.flow.model.embeddable.GraphDeployment;

/**
 * Configuration component that maintains what graphs we're configured to deploy
 * after registration.
 */
@Component
@ConditionalOnProperty(
    value = "ubiquia.bootstrap.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@ConfigurationProperties("ubiquia.bootstrap.graph")
public class GraphDeployments {

    private List<GraphDeployment> deployments;

    public List<GraphDeployment> getDeployments() {
        return deployments;
    }

    public void setDeployments(List<GraphDeployment> deployments) {
        this.deployments = deployments;
    }
}
