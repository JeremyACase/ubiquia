package org.ubiquia.common.model.ubiquia.entity;


import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import org.ubiquia.common.model.ubiquia.embeddable.*;
import org.ubiquia.common.model.ubiquia.enums.NodeType;

@Entity
public class NodeEntity extends AbstractModelEntity {

    private NodeType nodeType;

    private NodeSettings nodeSettings;

    private CommunicationServiceSettings communicationServiceSettings;

    @OneToMany(mappedBy = "node", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private List<FlowEventEntity> flowEvents;

    private BrokerSettings brokerSettings;

    @OneToOne
    @JoinColumn(name = "component_node_join_id", nullable = true)
    private ComponentEntity component;

    private String description;

    private String endpoint;

    private EgressSettings egressSettings;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graph_node_join_id", nullable = false)
    private GraphEntity graph;

    @ElementCollection
    private Set<SubSchema> inputSubSchemas;

    private SubSchema outputSubSchema;

    private String name;

    @ElementCollection
    private Set<OverrideSettingsStringified> overrideSettings;

    private PollSettings pollSettings;

    @ManyToMany(
        fetch = FetchType.EAGER,
        cascade = CascadeType.REFRESH,
        targetEntity = NodeEntity.class)
    @JoinTable(name = "upstream_downstream_join_id",
        joinColumns = {@JoinColumn(name = "upstream_id")},
        inverseJoinColumns = {@JoinColumn(name = "downstream_id")})
    @Valid
    private List<NodeEntity> upstreamNodes;

    @ManyToMany(mappedBy = "upstreamNodes", fetch = FetchType.EAGER, cascade = CascadeType.REFRESH)
    private List<NodeEntity> downstreamNodes;

    @OneToMany(mappedBy = "targetNode", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private List<FlowMessageEntity> outboxFlowMessages;

    public ComponentEntity getComponent() {
        return component;
    }

    public void setComponent(ComponentEntity component) {
        this.component = component;
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
    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public List<NodeEntity> getUpstreamNodes() {
        return upstreamNodes;
    }

    public void setUpstreamNodes(List<NodeEntity> upstreamNodes) {
        this.upstreamNodes = upstreamNodes;
    }

    public List<NodeEntity> getDownstreamNodes() {
        return downstreamNodes;
    }

    public void setDownstreamNodes(List<NodeEntity> downstreamAdapter) {
        this.downstreamNodes = downstreamAdapter;
    }

    public List<FlowMessageEntity> getOutboxFlowMessages() {
        return outboxFlowMessages;
    }

    public void setOutboxFlowMessages(List<FlowMessageEntity> outboxFlowMessages) {
        this.outboxFlowMessages = outboxFlowMessages;
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
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NotNull
    public NodeSettings getNodeSettings() {
        return nodeSettings;
    }

    public void setNodeSettings(NodeSettings nodeSettings) {
        this.nodeSettings = nodeSettings;
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
