package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.ubiquia.common.model.ubiquia.embeddable.*;
import org.ubiquia.common.model.ubiquia.enums.NodeType;

public class Node extends AbstractModel {

    private String name;

    private NodeSettings nodeSettings;

    private NodeType nodeType;

    private List<FlowEvent> flowEvents;

    private BrokerSettings brokerSettings;

    private Component component;

    private CommunicationServiceSettings communicationServiceSettings;

    private String description;

    private String endpoint;

    private EgressSettings egressSettings;

    private Graph graph;

    private List<SubSchema> inputSubSchemas;

    private SubSchema outputSubSchema;

    private List<OverrideSettings> overrideSettings;

    private PollSettings pollSettings;

    private List<Node> upstreamNodes;

    private List<Node> downstreamNodes;

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
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

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @NotNull
    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public List<Node> getUpstreamNodes() {
        return upstreamNodes;
    }

    public void setUpstreamNodes(List<Node> upstreamNodes) {
        this.upstreamNodes = upstreamNodes;
    }

    public List<Node> getDownstreamNodes() {
        return downstreamNodes;
    }

    public void setDownstreamNodes(List<Node> downstreamNodes) {
        this.downstreamNodes = downstreamNodes;
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
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NodeSettings getNodeSettings() {
        return nodeSettings;
    }

    public void setNodeSettings(NodeSettings nodeSettings) {
        this.nodeSettings = nodeSettings;
    }

    public List<OverrideSettings> getOverrideSettings() {
        return overrideSettings;
    }

    public void setOverrideSettings(List<OverrideSettings> overrideSettings) {
        this.overrideSettings = overrideSettings;
    }

    @Override
    public String getModelType() {
        return "Node";
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

    public CommunicationServiceSettings getCommunicationServiceSettings() {
        return communicationServiceSettings;
    }

    public void setCommunicationServiceSettings(CommunicationServiceSettings communicationServiceSettings) {
        this.communicationServiceSettings = communicationServiceSettings;
    }
}
