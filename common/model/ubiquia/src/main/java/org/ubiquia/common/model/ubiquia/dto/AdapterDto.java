package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.ubiquia.common.model.ubiquia.embeddable.*;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;

public class AdapterDto extends AbstractEntityDto {

    private String adapterName;

    private AdapterSettings adapterSettings;

    private AdapterType adapterType;

    private List<FlowEventDto> flowEvents;

    private BrokerSettings brokerSettings;

    private AgentDto agent;

    private String description;

    private String endpoint;

    private EgressSettings egressSettings;

    private GraphDto graph;

    private List<SubSchema> inputSubSchemas;

    private SubSchema outputSubSchema;

    private List<OverrideSettings> overrideSettings;

    private PollSettings pollSettings;

    private List<AdapterDto> upstreamAdapters;

    private List<AdapterDto> downstreamAdapters;

    public AgentDto getAgent() {
        return agent;
    }

    public void setAgent(AgentDto agent) {
        this.agent = agent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public GraphDto getGraph() {
        return graph;
    }

    public void setGraph(GraphDto graph) {
        this.graph = graph;
    }

    @NotNull
    public AdapterType getAdapterType() {
        return adapterType;
    }

    public void setAdapterType(AdapterType adapterType) {
        this.adapterType = adapterType;
    }

    public List<AdapterDto> getUpstreamAdapters() {
        return upstreamAdapters;
    }

    public void setUpstreamAdapters(List<AdapterDto> upstreamAdapters) {
        this.upstreamAdapters = upstreamAdapters;
    }

    public List<AdapterDto> getDownstreamAdapters() {
        return downstreamAdapters;
    }

    public void setDownstreamAdapters(List<AdapterDto> downstreamAdapters) {
        this.downstreamAdapters = downstreamAdapters;
    }

    public List<FlowEventDto> getFlowEvents() {
        return flowEvents;
    }

    public void setFlowEvents(List<FlowEventDto> flowEvents) {
        this.flowEvents = flowEvents;
    }

    public BrokerSettings getBrokerSettings() {
        return brokerSettings;
    }

    public void setBrokerSettings(BrokerSettings brokerSettings) {
        this.brokerSettings = brokerSettings;
    }

    public EgressSettings getEgressSettings() {
        return egressSettings;
    }

    public void setEgressSettings(EgressSettings egressSettings) {
        this.egressSettings = egressSettings;
    }

    public PollSettings getPollSettings() {
        return pollSettings;
    }

    public void setPollSettings(PollSettings pollSettings) {
        this.pollSettings = pollSettings;
    }

    @NotNull
    public String getAdapterName() {
        return adapterName;
    }

    public void setAdapterName(String adapterName) {
        this.adapterName = adapterName;
    }

    public AdapterSettings getAdapterSettings() {
        return adapterSettings;
    }

    public void setAdapterSettings(AdapterSettings adapterSettings) {
        this.adapterSettings = adapterSettings;
    }

    public List<OverrideSettings> getOverrideSettings() {
        return overrideSettings;
    }

    public void setOverrideSettings(List<OverrideSettings> overrideSettings) {
        this.overrideSettings = overrideSettings;
    }

    @Override
    public String getModelType() {
        return "Adapter";
    }

    public List<SubSchema> getInputSubSchemas() {
        return inputSubSchemas;
    }

    public void setInputSubSchemas(List<SubSchema> inputSubSchemas) {
        this.inputSubSchemas = inputSubSchemas;
    }

    public SubSchema getOutputSubSchema() {
        return outputSubSchema;
    }

    public void setOutputSubSchema(SubSchema outputSubSchema) {
        this.outputSubSchema = outputSubSchema;
    }
}
