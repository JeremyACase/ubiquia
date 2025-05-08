package org.ubiquia.core.flow.model.embeddable;


import jakarta.persistence.Embeddable;
import org.springframework.validation.annotation.Validated;

@Validated
@Embeddable
public class GraphSettings {

    private String flag;

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

}
