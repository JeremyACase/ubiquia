package org.ubiquia.common.model.ubiquia.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.common.model.ubiquia.embeddable.FlowEventTimes;
import org.ubiquia.common.model.ubiquia.embeddable.KeyValuePair;

@Validated
@Entity
public class FlowEventEntity extends AbstractModelEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adapter_event_join_id", nullable = false)
    private AdapterEntity adapter;

    private String flowId;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String inputPayload;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String outputPayload;

    private Integer httpResponseCode;

    private FlowEventTimes flowEventTimes;

    @OneToMany(mappedBy = "flowEvent", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private Set<FlowMessageEntity> flowMessages;

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

    @Pattern(regexp = "^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$")
    @NotNull
    public String getFlowId() {
        return this.flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    @NotNull
    public AdapterEntity getAdapter() {
        return adapter;
    }

    public void setAdapter(AdapterEntity adapterEntity) {
        this.adapter = adapterEntity;
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

    public Set<FlowMessageEntity> getFlowMessages() {
        return flowMessages;
    }

    public void setFlowMessages(Set<FlowMessageEntity> flowMessageEntities) {
        this.flowMessages = flowMessageEntities;
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
