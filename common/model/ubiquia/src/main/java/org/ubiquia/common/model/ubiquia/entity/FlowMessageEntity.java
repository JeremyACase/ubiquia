package org.ubiquia.common.model.ubiquia.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
public class FlowMessageEntity extends AbstractModelEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "message_events_join_id", nullable = false)
    private FlowEventEntity flowEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_target_adapter_join_id", nullable = false)
    private AdapterEntity targetAdapter;

    @Column(columnDefinition = "LONGTEXT")
    private String payload;

    @NotNull
    public FlowEventEntity getFlowEvent() {
        return flowEvent;
    }

    public void setFlowEvent(FlowEventEntity flowEventEntity) {
        this.flowEvent = flowEventEntity;
    }

    @NotNull
    public AdapterEntity getTargetAdapter() {
        return targetAdapter;
    }

    public void setTargetAdapter(AdapterEntity targetAdapterEntity) {
        this.targetAdapter = targetAdapterEntity;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
