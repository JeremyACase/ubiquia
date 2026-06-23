package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.NotNull;

/** FlowMessage model. */
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

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @NotNull
    public Node getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(Node targetNode) {
        this.targetNode = targetNode;
    }
}
