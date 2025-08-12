package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.NotNull;

public class FlowMessage extends AbstractModel {

    private FlowEvent flowEvent;

    private Adapter targetAdapter;

    private String payload;

    @Override
    public String getModelType() {
        return "FlowMessage";
    }

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
