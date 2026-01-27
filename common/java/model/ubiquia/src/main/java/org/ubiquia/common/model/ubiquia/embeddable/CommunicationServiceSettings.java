package org.ubiquia.common.model.ubiquia.embeddable;


import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
@Embeddable
public class CommunicationServiceSettings {

    private Boolean exposeViaCommService = false;

    private String proxiedEndpoint;

    @NotNull
    public Boolean getExposeViaCommService() {
        return exposeViaCommService;
    }

    public void setExposeViaCommService(Boolean exposeViaCommService) {
        this.exposeViaCommService = exposeViaCommService;
    }

    @NotNull
    public String getProxiedEndpoint() {
        return proxiedEndpoint;
    }

    public void setProxiedEndpoint(String proxiedEndpoint) {
        this.proxiedEndpoint = proxiedEndpoint;
    }
}
