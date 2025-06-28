package org.ubiquia.common.model.ubiquia.entity;


import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import org.ubiquia.common.model.ubiquia.embeddable.*;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;

@Entity
public class AdapterEntity extends AbstractModelEntity {

    private AdapterType adapterType;

    private AdapterSettings adapterSettings;

    private CommunicationServiceSettings communicationServiceSettings;

    @OneToMany(mappedBy = "adapter", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private List<FlowEventEntity> flowEvents;

    private BrokerSettings brokerSettings;

    @OneToOne
    @JoinColumn(name = "agent_adapter_join_id", nullable = true)
    private AgentEntity agent;

    private String description;

    private String endpoint;

    private EgressSettings egressSettings;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graph_adapter_join_id", nullable = false)
    private GraphEntity graph;

    @ElementCollection
    private Set<SubSchema> inputSubSchemas;

    private SubSchema outputSubSchema;

    private String adapterName;

    @ElementCollection
    private Set<OverrideSettingsStringified> overrideSettings;

    private PollSettings pollSettings;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.REFRESH, targetEntity = AdapterEntity.class)
    @JoinTable(name = "upstream_downstream_join_id",
        joinColumns = {@JoinColumn(name = "upstream_id")},
        inverseJoinColumns = {@JoinColumn(name = "downstream_id")})
    @Valid
    private List<AdapterEntity> upstreamAdapters;

    @ManyToMany(mappedBy = "upstreamAdapters", fetch = FetchType.EAGER, cascade = CascadeType.REFRESH)
    private List<AdapterEntity> downstreamAdapters;

    @OneToMany(mappedBy = "targetAdapter", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private List<FlowMessageEntity> outboxMessages;

    public AgentEntity getAgent() {
        return agent;
    }

    public void setAgent(AgentEntity agent) {
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
    public GraphEntity getGraph() {
        return graph;
    }

    public void setGraph(GraphEntity graph) {
        this.graph = graph;
    }

    @NotNull
    public AdapterType getAdapterType() {
        return adapterType;
    }

    public void setAdapterType(AdapterType adapterType) {
        this.adapterType = adapterType;
    }

    public List<AdapterEntity> getUpstreamAdapters() {
        return upstreamAdapters;
    }

    public void setUpstreamAdapters(List<AdapterEntity> upstreamAdapters) {
        this.upstreamAdapters = upstreamAdapters;
    }

    public List<AdapterEntity> getDownstreamAdapters() {
        return downstreamAdapters;
    }

    public void setDownstreamAdapters(List<AdapterEntity> downstreamAdapter) {
        this.downstreamAdapters = downstreamAdapter;
    }

    public List<FlowMessageEntity> getOutboxMessages() {
        return outboxMessages;
    }

    public void setOutboxMessages(List<FlowMessageEntity> outboxMessages) {
        this.outboxMessages = outboxMessages;
    }

    public List<FlowEventEntity> getFlowEvents() {
        return flowEvents;
    }

    public void setFlowEvents(List<FlowEventEntity> flowEvents) {
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

    @NotNull
    public CommunicationServiceSettings getCommunicationServiceSettings() {
        return communicationServiceSettings;
    }

    public void setCommunicationServiceSettings(CommunicationServiceSettings communicationServiceSettings) {
        this.communicationServiceSettings = communicationServiceSettings;
    }
}
