package org.ubiquia.common.model.ubiquia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.ubiquia.common.model.ubiquia.embeddable.FlowEventTimes;

public class FlowEventDto extends AbstractEntityDto {

    private AdapterDto adapter;

    private String batchId;

    private Object inputPayload;

    private Object outputPayload;

    private Integer httpResponseCode;

    private FlowEventTimes flowEventTimes;

    private List<KeyValuePairDto> inputPayloadStamps;

    private List<KeyValuePairDto> outputPayloadStamps;

    @Schema(readOnly = true)
    @Pattern(regexp = "[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}")
    public String getBatchId() {
        return this.batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    @Override
    public String getModelType() {
        return "FlowEvent";
    }

    public AdapterDto getAdapter() {
        return adapter;
    }

    public void setAdapter(AdapterDto adapter) {
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

    public List<KeyValuePairDto> getInputPayloadStamps() {
        return inputPayloadStamps;
    }

    public void setInputPayloadStamps(List<KeyValuePairDto> inputPayloadStamps) {
        this.inputPayloadStamps = inputPayloadStamps;
    }

    public List<KeyValuePairDto> getOutputPayloadStamps() {
        return outputPayloadStamps;
    }

    public void setOutputPayloadStamps(List<KeyValuePairDto> outputPayloadStamps) {
        this.outputPayloadStamps = outputPayloadStamps;
    }

    public FlowEventTimes getFlowEventTimes() {
        return flowEventTimes;
    }

    public void setFlowEventTimes(FlowEventTimes flowEventTimes) {
        this.flowEventTimes = flowEventTimes;
    }
}
