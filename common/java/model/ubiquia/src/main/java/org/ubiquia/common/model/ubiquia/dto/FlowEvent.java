package org.ubiquia.common.model.ubiquia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.ubiquia.common.model.ubiquia.embeddable.FlowEventTimes;

public class FlowEvent extends AbstractModel {

    private Adapter adapter;

    private String flowId;

    private Object inputPayload;

    private Object outputPayload;

    private Integer httpResponseCode;

    private FlowEventTimes flowEventTimes;

    private List<KeyValuePair> inputPayloadStamps;

    private List<KeyValuePair> outputPayloadStamps;

    @Schema(readOnly = true)
    @Pattern(regexp = "[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}")
    public String getFlowId() {
        return this.flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    @Override
    public String getModelType() {
        return "FlowEvent";
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
    }

    public Object getInputPayload() {
        return inputPayload;
    }

    public void setInputPayload(Object inputPayload) {
        this.inputPayload = inputPayload;
    }

    public Object getOutputPayload() {
        return outputPayload;
    }

    public void setOutputPayload(Object outputPayload) {
        this.outputPayload = outputPayload;
    }

    public Integer getHttpResponseCode() {
        return httpResponseCode;
    }

    public void setHttpResponseCode(Integer httpResponseCode) {
        this.httpResponseCode = httpResponseCode;
    }

    public List<KeyValuePair> getInputPayloadStamps() {
        return inputPayloadStamps;
    }

    public void setInputPayloadStamps(List<KeyValuePair> inputPayloadStamps) {
        this.inputPayloadStamps = inputPayloadStamps;
    }

    public List<KeyValuePair> getOutputPayloadStamps() {
        return outputPayloadStamps;
    }

    public void setOutputPayloadStamps(List<KeyValuePair> outputPayloadStamps) {
        this.outputPayloadStamps = outputPayloadStamps;
    }

    public FlowEventTimes getFlowEventTimes() {
        return flowEventTimes;
    }

    public void setFlowEventTimes(FlowEventTimes flowEventTimes) {
        this.flowEventTimes = flowEventTimes;
    }
}
