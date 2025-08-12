package org.ubiquia.core.flow.component.config.bootstrap;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;

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
@ConfigurationProperties("ubiquia.agent.flow-service.bootstrap.graph")
@Validated
public class GraphBootstrapConfig {

    private Boolean enabled;

    private String directory;

    private List<GraphDeployment> deployments;

    public List<GraphDeployment> getDeployments() {
        return deployments;
    }

    public void setDeployments(List<GraphDeployment> deployments) {
        this.deployments = deployments;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }
}
