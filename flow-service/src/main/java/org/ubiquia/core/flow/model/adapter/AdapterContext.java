package org.ubiquia.core.flow.model.adapter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.ubiquia.core.flow.model.embeddable.*;
import org.ubiquia.core.flow.model.enums.AdapterType;

/**
 * A class that maintains several data points for adapters.
 */
public class AdapterContext {

    private String adapterName;

    private AdapterSettings adapterSettings;

    private AdapterType adapterType;

    private Long backpressurePollRatePerMinute;

    private List<Long> backPressureSamplings = new ArrayList<>();

    private BrokerSettings brokerSettings;

    private String agentName;

    private GraphSettings graphSettings;

    private EgressSettings egressSettings;

    private String graphName;
    private String adapterId;
    private Boolean isTemplateTransform;
    private PollSettings pollSettings;
    private List<RequestMappingInfo> registeredMappingInfos = new ArrayList<>();
    private List<KeyValuePair> tags = new ArrayList<>();
    private List<ScheduledFuture> tasks = new ArrayList<>();
    private URI endpointUri;
    private Integer openMessages = 0;

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

    public AdapterType getAdapterType() {
        return adapterType;
    }

    public void setAdapterType(AdapterType adapterType) {
        this.adapterType = adapterType;
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

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
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

    public String getGraphName() {
        return graphName;
    }

    public void setGraphName(String graphName) {
        this.graphName = graphName;
    }

    public String getAdapterId() {
        return adapterId;
    }

    public void setAdapterId(String adapterId) {
        this.adapterId = adapterId;
    }

    public Boolean getTemplateTransform() {
        return isTemplateTransform;
    }

    public void setTemplateTransform(Boolean templateTransform) {
        isTemplateTransform = templateTransform;
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
}
