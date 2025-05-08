package org.ubiquia.core.flow.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.core.flow.model.embeddable.FlowEventTimes;

@Validated
public class FlowEventDto extends AbstractEntityDto {

    @JsonProperty("adapter")
    private AdapterDto adapter;

    @JsonProperty("batchId")
    private String batchId;

    @JsonProperty("inputPayload")
    private Object inputPayload;

    @JsonProperty("outputPayload")
    private Object outputPayload;

    @JsonProperty("httpResponseCode")
    private Integer httpResponseCode;

    @JsonProperty("flowEventTimes")
    private FlowEventTimes flowEventTimes;

    @JsonProperty("inputPayloadStamps")
    private List<KeyValuePairDto> inputPayloadStamps;

    @JsonProperty("outputPayloadStamps")
    private List<KeyValuePairDto> outputPayloadStamps;

    @ApiModelProperty(
        readOnly = true,
        value = "")
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
