package org.ubiquia.common.model.ubiquia.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
public class FlowMessage extends AbstractEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "message_events_join_id", nullable = false)
    private FlowEvent flowEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_target_adapter_join_id", nullable = false)
    private Adapter targetAdapter;

    @Column(columnDefinition = "LONGTEXT")
    private String payload;

    @NotNull
    public FlowEvent getFlowEvent() {
        return flowEvent;
    }

    public void setFlowEvent(FlowEvent flowEvent) {
        this.flowEvent = flowEvent;
    }

    @NotNull
    public Adapter getTargetAdapter() {
        return targetAdapter;
    }

    public void setTargetAdapter(Adapter targetAdapter) {
        this.targetAdapter = targetAdapter;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
