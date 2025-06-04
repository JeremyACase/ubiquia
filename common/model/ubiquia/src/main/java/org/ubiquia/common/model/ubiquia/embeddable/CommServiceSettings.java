package org.ubiquia.common.model.ubiquia.embeddable;


import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.common.model.ubiquia.enums.BrokerType;

@Validated
@Embeddable
public class CommServiceSettings {

    private Boolean exposeViaCommService = false;

    @NotNull
    public Boolean getExposeViaCommService() {
        return exposeViaCommService;
    }

    public void setExposeViaCommService(Boolean exposeViaCommService) {
        this.exposeViaCommService = exposeViaCommService;
    }
}
