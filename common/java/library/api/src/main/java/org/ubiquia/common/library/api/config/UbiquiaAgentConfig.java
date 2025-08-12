package org.ubiquia.common.library.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * A bean to be used for service configuration. Makes for consistent Helm injection.
 */
@Component
@ConfigurationProperties(prefix = "ubiquia.agent")
public class UbiquiaAgentConfig {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
