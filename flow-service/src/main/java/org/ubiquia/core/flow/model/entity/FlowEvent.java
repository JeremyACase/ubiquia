package org.ubiquia.core.flow.model.entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.Set;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.core.flow.model.embeddable.KeyValuePair;

@Validated
@Entity
public class FlowEvent extends AbstractEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adapter_event_join_id", nullable = false)
    private Adapter adapter;

    private String batchId;

    @Column(columnDefinition = "LONGTEXT")
    private String inputPayload;

    @Column(columnDefinition = "LONGTEXT")
    private String outputPayload;

    private Integer httpResponseCode;

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

    @OneToMany(mappedBy = "amigosEvent", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private Set<FlowMessage> flowMessages;

    @ElementCollection
    @AttributeOverrides({
        @AttributeOverride(
            name = "key",
            column = @Column(name = "input_payload_stamp_key")),
        @AttributeOverride(
            name = "value",
            column = @Column(name = "input_payload_stamp_value"))
    })
    private Set<KeyValuePair> inputPayloadStamps;

    @ElementCollection
    @AttributeOverrides({
        @AttributeOverride(
            name = "key",
            column = @Column(name = "output_payload_stamp_key")),
        @AttributeOverride(
            name = "value",
            column = @Column(name = "output_payload_stamp_value"))
    })
    private Set<KeyValuePair> outputPayloadStamps;

    @ApiModelProperty(
        readOnly = true,
        value = "")
    @Pattern(regexp = "[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}")
    @NotNull
    public String getBatchId() {
        return this.batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    @NotNull
    public Adapter getAdapter() {
        return adapter;
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
    }

    public String getInputPayload() {
        return inputPayload;
    }

    public void setInputPayload(String inputPayload) {
        this.inputPayload = inputPayload;
    }

    public String getOutputPayload() {
        return outputPayload;
    }

    public void setOutputPayload(String outputPayload) {
        this.outputPayload = outputPayload;
    }

    public Integer getHttpResponseCode() {
        return httpResponseCode;
    }

    public void setHttpResponseCode(Integer httpResponseCode) {
        this.httpResponseCode = httpResponseCode;
    }

    public Set<FlowMessage> getAmigosMessages() {
        return flowMessages;
    }

    public void setAmigosMessages(Set<FlowMessage> flowMessages) {
        this.flowMessages = flowMessages;
    }

    public Set<KeyValuePair> getInputPayloadStamps() {
        return inputPayloadStamps;
    }

    public void setInputPayloadStamps(Set<KeyValuePair> inputPayloadStamps) {
        this.inputPayloadStamps = inputPayloadStamps;
    }

    public Set<KeyValuePair> getOutputPayloadStamps() {
        return outputPayloadStamps;
    }

    public void setOutputPayloadStamps(Set<KeyValuePair> outputPayloadStamps) {
        this.outputPayloadStamps = outputPayloadStamps;
    }

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
}
