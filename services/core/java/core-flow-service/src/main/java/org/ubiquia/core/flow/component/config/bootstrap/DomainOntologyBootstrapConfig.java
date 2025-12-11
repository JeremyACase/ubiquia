package org.ubiquia.core.flow.component.config.bootstrap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration component that maintains what ontologies we're configured to deploy
 * after registration.
 */
@Component
@ConditionalOnProperty(
    value = "ubiquia.agent.flow-service.bootstrap.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@ConfigurationProperties("ubiquia.agent.flow-service.bootstrap.domain-ontology")
@Validated
public class DomainOntologyBootstrapConfig {

    private Boolean enabled;

    private String directory;

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