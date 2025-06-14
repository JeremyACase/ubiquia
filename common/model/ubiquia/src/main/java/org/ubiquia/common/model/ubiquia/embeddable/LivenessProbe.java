package org.ubiquia.common.model.ubiquia.embeddable;


import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Embeddable
public class LivenessProbe {

    private String httpGetPath;

    private Integer initialDelaySeconds;

    @NotNull
    public String getHttpGetPath() {
        return httpGetPath;
    }

    public void setHttpGetPath(String httpGetPath) {
        this.httpGetPath = httpGetPath;
    }

    @NotNull
    @Min(1)
    public Integer getInitialDelaySeconds() {
        return initialDelaySeconds;
    }

    public void setInitialDelaySeconds(Integer initialDelaySeconds) {
        this.initialDelaySeconds = initialDelaySeconds;
    }
}
