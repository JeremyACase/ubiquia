package org.ubiquia.core.flow.model.embeddable;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.core.flow.model.enums.BrokerType;

@Validated
@Embeddable
public class BrokerSettings {

    @JsonProperty("type")
    private BrokerType type;

    @JsonProperty("topic")
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
