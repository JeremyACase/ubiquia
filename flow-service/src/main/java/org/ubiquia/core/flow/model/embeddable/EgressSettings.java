package org.ubiquia.core.flow.model.embeddable;


import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.core.flow.model.enums.EgressType;

@Validated
@Embeddable
public class EgressSettings {

    private EgressType egressType = EgressType.POST;

    private Integer egressConcurrency = 1;

    @NotNull
    public EgressType getEgressType() {
        return egressType;
    }

    public void setEgressType(EgressType egressType) {
        this.egressType = egressType;
    }

    @NotNull
    @Min(1)
    public Integer getEgressConcurrency() {
        return egressConcurrency;
    }

    public void setEgressConcurrency(Integer egressConcurrency) {
        this.egressConcurrency = egressConcurrency;
    }
}
