package org.ubiquia.common.models.embeddable;


import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.common.models.enums.BrokerType;

@Validated
@Embeddable
public class BrokerSettings {

    private BrokerType type;

    private String topic;

    @NotNull
    public BrokerType getType() {
        return type;
    }

    public void setType(BrokerType type) {
        this.type = type;
    }

    @NotNull
    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
