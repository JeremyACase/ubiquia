package org.ubiquia.core.communication.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Binds {@code ubiquia.dashboard.service.*} configuration properties. */
@Component
@ConfigurationProperties(prefix = "ubiquia.dashboard.service")
public class DashboardServiceConfig {

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

    public String getBaseUrl() {
        return this.url + ":" + this.port;
    }
}
