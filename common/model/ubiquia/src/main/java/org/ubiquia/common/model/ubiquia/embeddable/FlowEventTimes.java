package org.ubiquia.common.model.ubiquia.embeddable;


import jakarta.persistence.Embeddable;
import java.time.OffsetDateTime;

@Embeddable
public class FlowEventTimes {

    private OffsetDateTime agentResponseTime;

    private OffsetDateTime payloadSentToAgentTime;

    private OffsetDateTime eventStartTime;

    private OffsetDateTime eventCompleteTime;

    private OffsetDateTime egressResponseReceivedTime;

    private OffsetDateTime sentToOutboxTime;

    private OffsetDateTime payloadEgressedTime;

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
