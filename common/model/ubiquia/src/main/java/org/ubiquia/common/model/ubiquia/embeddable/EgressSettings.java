package org.ubiquia.common.model.ubiquia.embeddable;


import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.ubiquia.common.model.ubiquia.enums.EgressType;
import org.ubiquia.common.model.ubiquia.enums.HttpOutputType;

@Embeddable
public class EgressSettings {

    private EgressType egressType = EgressType.SYNCHRONOUS;

    private HttpOutputType httpOutputType = HttpOutputType.POST;

    private Integer egressConcurrency = 1;

    @NotNull
    public EgressType getEgressType() {
        return egressType;
    }

    @NotNull
    @Min(1)
    public Integer getEgressConcurrency() {
        return egressConcurrency;
    }

    public void setEgressType(EgressType egressType) {
        this.egressType = egressType;
    }

    public HttpOutputType getHttpOutputType() {
        return httpOutputType;
    }

    public void setHttpOutputType(HttpOutputType httpOutputType) {
        this.httpOutputType = httpOutputType;
    }

    public void setEgressConcurrency(Integer egressConcurrency) {
        this.egressConcurrency = egressConcurrency;
    }
}
