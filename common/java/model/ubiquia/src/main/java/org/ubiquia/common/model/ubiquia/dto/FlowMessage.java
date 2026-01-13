package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.NotNull;

public class FlowMessage extends AbstractModel {

    private FlowEvent flowEvent;

    private Node targetNode;

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
    public Node getTargetAdapter() {
        return targetNode;
    }

    public void setTargetAdapter(Node targetNode) {
        this.targetNode = targetNode;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
