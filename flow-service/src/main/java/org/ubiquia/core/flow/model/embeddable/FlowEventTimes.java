package org.ubiquia.core.flow.model.embeddable;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import java.time.OffsetDateTime;
import org.springframework.validation.annotation.Validated;

@Embeddable
public class FlowEventTimes {

    @JsonProperty("payloadReceivedTime")
    private OffsetDateTime payloadReceivedTime;

    @JsonProperty("payloadSentToTransformTime")
    private OffsetDateTime payloadSentToTransformTime;

    @JsonProperty("eventStartTime")
    private OffsetDateTime eventStartTime;

    @JsonProperty("eventCompleteTime")
    private OffsetDateTime eventCompleteTime;

    @JsonProperty("egressResponseReceivedTime")
    private OffsetDateTime egressResponseReceivedTime;

    @JsonProperty("dataTransformResponseTime")
    private OffsetDateTime dataTransformResponseTime;

    @JsonProperty("payloadSentToOutboxTime")
    private OffsetDateTime sentToOutboxTime;

    @JsonProperty("payloadEgressedTime")
    private OffsetDateTime payloadEgressedTime;

    @JsonProperty("pollStartedTime")
    private OffsetDateTime pollStartedTime;

    public OffsetDateTime getPayloadReceivedTime() {
        return payloadReceivedTime;
    }

    public void setPayloadReceivedTime(OffsetDateTime payloadReceivedTime) {
        this.payloadReceivedTime = payloadReceivedTime;
    }

    public OffsetDateTime getPayloadSentToTransformTime() {
        return payloadSentToTransformTime;
    }

    public void setPayloadSentToTransformTime(OffsetDateTime payloadSentToTransformTime) {
        this.payloadSentToTransformTime = payloadSentToTransformTime;
    }

    public OffsetDateTime getEventStartTime() {
        return eventStartTime;
    }

    public void setEventStartTime(OffsetDateTime eventStartTime) {
        this.eventStartTime = eventStartTime;
    }

    public OffsetDateTime getEventCompleteTime() {
        return eventCompleteTime;
    }

    public void setEventCompleteTime(OffsetDateTime eventCompleteTime) {
        this.eventCompleteTime = eventCompleteTime;
    }

    public OffsetDateTime getEgressResponseReceivedTime() {
        return egressResponseReceivedTime;
    }

    public void setEgressResponseReceivedTime(OffsetDateTime egressResponseReceivedTime) {
        this.egressResponseReceivedTime = egressResponseReceivedTime;
    }

    public OffsetDateTime getDataTransformResponseTime() {
        return dataTransformResponseTime;
    }

    public void setDataTransformResponseTime(OffsetDateTime dataTransformResponseTime) {
        this.dataTransformResponseTime = dataTransformResponseTime;
    }

    public OffsetDateTime getSentToOutboxTime() {
        return sentToOutboxTime;
    }

    public void setSentToOutboxTime(OffsetDateTime sentToOutboxTime) {
        this.sentToOutboxTime = sentToOutboxTime;
    }

    public OffsetDateTime getPayloadEgressedTime() {
        return payloadEgressedTime;
    }

    public void setPayloadEgressedTime(OffsetDateTime payloadEgressedTime) {
        this.payloadEgressedTime = payloadEgressedTime;
    }

    public OffsetDateTime getPollStartedTime() {
        return pollStartedTime;
    }

    public void setPollStartedTime(OffsetDateTime pollStartedTime) {
        this.pollStartedTime = pollStartedTime;
    }
}
