package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.NotNull;

public class FlowMessageDto extends AbstractEntityDto {

    private FlowEventDto flowEvent;

    private AdapterDto targetAdapter;

    private String payload;

    @Override
    public String getModelType() {
        return "FlowMessage";
    }

    @NotNull
    public FlowEventDto getFlowEvent() {
        return flowEvent;
    }

    public void setFlowEvent(FlowEventDto flowEvent) {
        this.flowEvent = flowEvent;
    }

    @NotNull
    public AdapterDto getTargetAdapter() {
        return targetAdapter;
    }

    public void setTargetAdapter(AdapterDto targetAdapter) {
        this.targetAdapter = targetAdapter;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
