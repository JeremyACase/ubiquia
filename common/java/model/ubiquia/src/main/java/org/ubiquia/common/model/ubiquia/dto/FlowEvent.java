package org.ubiquia.common.model.ubiquia.dto;

import java.util.List;
import org.ubiquia.common.model.ubiquia.embeddable.FlowEventTimes;

public class FlowEvent extends AbstractModel {

    private Flow flow;

    private Node node;

    private Object inputPayload;

    private Object outputPayload;

    private Integer httpResponseCode;

    private FlowEventTimes flowEventTimes;

    private List<KeyValuePair> inputPayloadStamps;

    private List<KeyValuePair> outputPayloadStamps;

    @Override
    public String getModelType() {
        return "FlowEvent";
    }

    public Node getAdapter() {
        return node;
    }

    public void setAdapter(Node node) {
        this.node = node;
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

    public Flow getFlow() {
        return flow;
    }

    public void setFlow(Flow flow) {
        this.flow = flow;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }
}
