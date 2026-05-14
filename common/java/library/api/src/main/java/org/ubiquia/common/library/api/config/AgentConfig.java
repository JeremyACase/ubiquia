package org.ubiquia.common.library.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * A bean to be used for service configuration. Makes for consistent Helm injection.
 */
@Component
@ConfigurationProperties(prefix = "ubiquia.agent")
public class AgentConfig {

    private String id;

    private String baseUrl;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
