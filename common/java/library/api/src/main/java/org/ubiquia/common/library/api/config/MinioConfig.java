package org.ubiquia.common.library.api.config;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * A bean to be used for service configuration. Makes for consistent Helm injection.
 */
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    private String username;
    private String password;
    private String url;

    public String getUrl() {
        return Objects.nonNull(url) ? url : System.getenv("MINIO_URL");
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return Objects.nonNull(username) ? username : System.getenv("MINIO_ACCESS_KEY");
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return Objects.nonNull(password) ? password : System.getenv("MINIO_SECRET_KEY");
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
