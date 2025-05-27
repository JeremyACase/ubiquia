package org.ubiquia.common.models.embeddable;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import java.time.OffsetDateTime;

@Embeddable
public class FlowEventTimes {
    @JsonProperty("agentResponseTime")
    private OffsetDateTime agentResponseTime;

    @JsonProperty("payloadSentToAgentTime")
    private OffsetDateTime payloadSentToAgentTime;

    @JsonProperty("eventStartTime")
    private OffsetDateTime eventStartTime;

    @JsonProperty("eventCompleteTime")
    private OffsetDateTime eventCompleteTime;

    @JsonProperty("egressResponseReceivedTime")
    private OffsetDateTime egressResponseReceivedTime;

    @JsonProperty("payloadSentToOutboxTime")
    private OffsetDateTime sentToOutboxTime;

    @JsonProperty("payloadEgressedTime")
    private OffsetDateTime payloadEgressedTime;

    @JsonProperty("pollStartedTime")
    private OffsetDateTime pollStartedTime;

    public OffsetDateTime getPayloadSentToAgentTime() {
        return payloadSentToAgentTime;
    }

    public void setPayloadSentToAgentTime(OffsetDateTime payloadSentToAgentTime) {
        this.payloadSentToAgentTime = payloadSentToAgentTime;
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

    public OffsetDateTime getAgentResponseTime() {
        return agentResponseTime;
    }

    public void setAgentResponseTime(OffsetDateTime agentResponseTime) {
        this.agentResponseTime = agentResponseTime;
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
