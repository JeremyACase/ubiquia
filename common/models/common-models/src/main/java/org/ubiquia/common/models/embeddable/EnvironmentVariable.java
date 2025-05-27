package org.ubiquia.common.models.embeddable;


import jakarta.persistence.Embeddable;
import org.springframework.validation.annotation.Validated;

@Embeddable
public class EnvironmentVariable {

    private String name;

    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
