package org.ubiquia.common.library.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * A bean to be used for service configuration. Makes for consistent Helm injection.
 */
@Component
@ConfigurationProperties(prefix = "ubiquia.belief-state-generator.service")
public class BeliefStateGeneratorServiceConfig {

    private String url;

    private Integer port;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
