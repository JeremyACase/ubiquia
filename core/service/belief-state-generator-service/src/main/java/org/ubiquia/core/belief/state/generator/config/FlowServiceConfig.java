package org.ubiquia.core.communication.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@ConfigurationProperties(prefix = "ubiquia.flow.service")
public class FlowServiceConfig {

    private String url;

    private Integer port;

    private Long pollFrequencyMilliseconds;

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

    public Long getPollFrequencyMilliseconds() {
        return pollFrequencyMilliseconds;
    }

    public void setPollFrequencyMilliseconds(Long pollFrequencyMilliseconds) {
        this.pollFrequencyMilliseconds = pollFrequencyMilliseconds;
    }
}
