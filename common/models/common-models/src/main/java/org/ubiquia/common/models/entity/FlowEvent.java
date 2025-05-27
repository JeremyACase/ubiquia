package org.ubiquia.common.models.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Set;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.common.models.embeddable.FlowEventTimes;
import org.ubiquia.common.models.embeddable.KeyValuePair;

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

    private FlowEventTimes flowEventTimes;

    @OneToMany(mappedBy = "flowEvent", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
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

    public Set<FlowMessage> getFlowMessages() {
        return flowMessages;
    }

    public void setFlowMessages(Set<FlowMessage> flowMessages) {
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

    public FlowEventTimes getFlowEventTimes() {
        return flowEventTimes;
    }

    public void setFlowEventTimes(FlowEventTimes flowEventTimes) {
        this.flowEventTimes = flowEventTimes;
    }
}
