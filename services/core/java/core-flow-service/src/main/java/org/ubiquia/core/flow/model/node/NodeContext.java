package org.ubiquia.core.flow.model.node;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.ubiquia.common.model.ubiquia.dto.Component;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.embeddable.*;
import org.ubiquia.common.model.ubiquia.enums.NodeType;

/**
 * A class that maintains several data points for adapters.
 */
public class NodeContext {

    private String nodeName;

    private NodeSettings nodeSettings;

    private Component component;

    private Graph graph;

    private NodeType nodeType;

    private Long backpressurePollRatePerMinute;

    private List<Long> backPressureSamplings = new ArrayList<>();

    private BrokerSettings brokerSettings;

    private GraphSettings graphSettings;

    private EgressSettings egressSettings;

    private String nodeId;
    private PollSettings pollSettings;
    private List<RequestMappingInfo> registeredMappingInfos = new ArrayList<>();
    private List<KeyValuePair> tags = new ArrayList<>();
    private List<ScheduledFuture> tasks = new ArrayList<>();
    private URI endpointUri;
    private Integer openMessages = 0;
    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void setAdapterSettings(NodeSettings nodeSettings) {
        this.nodeSettings = nodeSettings;
    }

    public Long getBackpressurePollRatePerMinute() {
        return backpressurePollRatePerMinute;
    }

    public void setBackpressurePollRatePerMinute(Long backpressurePollRatePerMinute) {
        this.backpressurePollRatePerMinute = backpressurePollRatePerMinute;
    }

    public List<Long> getBackPressureSamplings() {
        return backPressureSamplings;
    }

    public void setBackPressureSamplings(List<Long> backPressureSamplings) {
        this.backPressureSamplings = backPressureSamplings;
    }

    public BrokerSettings getBrokerSettings() {
        return brokerSettings;
    }

    public void setBrokerSettings(BrokerSettings brokerSettings) {
        this.brokerSettings = brokerSettings;
    }

    public GraphSettings getGraphSettings() {
        return graphSettings;
    }

    public void setGraphSettings(GraphSettings graphSettings) {
        this.graphSettings = graphSettings;
    }

    public EgressSettings getEgressSettings() {
        return egressSettings;
    }

    public void setEgressSettings(EgressSettings egressSettings) {
        this.egressSettings = egressSettings;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public PollSettings getPollSettings() {
        return pollSettings;
    }

    public void setPollSettings(PollSettings pollSettings) {
        this.pollSettings = pollSettings;
    }

    public List<RequestMappingInfo> getRegisteredMappingInfos() {
        return registeredMappingInfos;
    }

    public void setRegisteredMappingInfos(List<RequestMappingInfo> registeredMappingInfos) {
        this.registeredMappingInfos = registeredMappingInfos;
    }

    public List<KeyValuePair> getTags() {
        return tags;
    }

    public void setTags(List<KeyValuePair> tags) {
        this.tags = tags;
    }

    public List<ScheduledFuture> getTasks() {
        return tasks;
    }

    public void setTasks(List<ScheduledFuture> tasks) {
        this.tasks = tasks;
    }

    public URI getEndpointUri() {
        return endpointUri;
    }

    public void setEndpointUri(URI endpointUri) {
        this.endpointUri = endpointUri;
    }

    public Integer getOpenMessages() {
        return openMessages;
    }

    public void setOpenMessages(Integer openMessages) {
        this.openMessages = openMessages;
    }

    public NodeSettings getNodeSettings() {
        return nodeSettings;
    }

    public void setNodeSettings(NodeSettings nodeSettings) {
        this.nodeSettings = nodeSettings;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }
}
