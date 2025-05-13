package org.ubiquia.core.flow.model.entity;


import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.core.flow.model.embeddable.*;
import org.ubiquia.core.flow.model.enums.AdapterType;

@Validated
@Entity
public class Adapter extends AbstractEntity {

    private AdapterType adapterType;

    private AdapterSettings adapterSettings;

    @OneToMany(mappedBy = "adapter", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private List<FlowEvent> flowEvents;

    private BrokerSettings brokerSettings;

    @OneToOne
    @JoinColumn(name = "data_transform_adapter_join_id", nullable = true)
    private Agent agent;

    private String description;

    private String endpoint;

    private EgressSettings egressSettings;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graph_adapter_join_id", nullable = false)
    private Graph graph;

    @ElementCollection
    private Set<SubSchema> inputSubSchemas;

    private SubSchema outputSubSchema;

    private String adapterName;

    @ElementCollection
    private Set<OverrideSettingsStringified> overrideSettings;

    private PollSettings pollSettings;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.REFRESH, targetEntity = Adapter.class)
    @JoinTable(name = "upstream_downstream_join_id",
        joinColumns = {@JoinColumn(name = "upstream_id")},
        inverseJoinColumns = {@JoinColumn(name = "downstream_id")})
    @Valid
    private List<Adapter> upstreamAdapters;

    @ManyToMany(mappedBy = "upstreamAdapters", fetch = FetchType.EAGER, cascade = CascadeType.REFRESH)
    private List<Adapter> downstreamAdapters;

    @OneToMany(mappedBy = "targetAdapter", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private List<FlowMessage> outboxMessages;

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
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

    @NotNull
    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @NotNull
    public AdapterType getAdapterType() {
        return adapterType;
    }

    public void setAdapterType(AdapterType adapterType) {
        this.adapterType = adapterType;
    }

    public List<Adapter> getUpstreamAdapters() {
        return upstreamAdapters;
    }

    public void setUpstreamAdapters(List<Adapter> upstreamAdapters) {
        this.upstreamAdapters = upstreamAdapters;
    }

    public List<Adapter> getDownstreamAdapters() {
        return downstreamAdapters;
    }

    public void setDownstreamAdapters(List<Adapter> downstreamAdapters) {
        this.downstreamAdapters = downstreamAdapters;
    }

    public List<FlowMessage> getOutboxMessages() {
        return outboxMessages;
    }

    public void setOutboxMessages(List<FlowMessage> outboxMessages) {
        this.outboxMessages = outboxMessages;
    }

    public List<FlowEvent> getFlowEvents() {
        return flowEvents;
    }

    public void setFlowEvents(List<FlowEvent> flowEvents) {
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

    @NotNull
    public AdapterSettings getAdapterSettings() {
        return adapterSettings;
    }

    public void setAdapterSettings(AdapterSettings adapterSettings) {
        this.adapterSettings = adapterSettings;
    }

    public Set<OverrideSettingsStringified> getOverrideSettings() {
        return overrideSettings;
    }

    public void setOverrideSettings(Set<OverrideSettingsStringified> overrideSettings) {
        this.overrideSettings = overrideSettings;
    }

    public Set<SubSchema> getInputSubSchemas() {
        return inputSubSchemas;
    }

    public void setInputSubSchemas(Set<SubSchema> inputSubSchemas) {
        this.inputSubSchemas = inputSubSchemas;
    }

    public SubSchema getOutputSubSchema() {
        return outputSubSchema;
    }

    public void setOutputSubSchema(SubSchema outputSubSchema) {
        this.outputSubSchema = outputSubSchema;
    }
}
