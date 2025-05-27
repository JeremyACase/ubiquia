package org.ubiquia.common.models.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.ubiquia.common.models.embeddable.*;
import org.ubiquia.common.models.enums.AgentType;

public class AgentDto extends AbstractEntityDto {

    private AgentType agentType;

    private ConfigDto config;

    private String agentName;

    private String description;

    private List<EnvironmentVariable> environmentVariables;

    private List<OverrideSettings> overrideSettings;

    private Integer port;

    private InitContainer initContainer;

    private LivenessProbe livenessProbe;

    private AdapterDto adapter;

    private GraphDto graph;

    private Image image;

    private List<Volume> volumes;

    private ScaleSettings scaleSettings;

    @NotNull
    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    @Override
    public String getModelType() {
        return "Agent";
    }

    @NotNull
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public AdapterDto getAdapter() {
        return adapter;
    }

    public void setAdapter(AdapterDto adapter) {
        this.adapter = adapter;
    }

    public GraphDto getGraph() {
        return graph;
    }

    public void setGraph(GraphDto graph) {
        this.graph = graph;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public InitContainer getInitContainer() {
        return initContainer;
    }

    public void setInitContainer(InitContainer initContainer) {
        this.initContainer = initContainer;
    }

    public LivenessProbe getLivenessProbe() {
        return livenessProbe;
    }

    public void setLivenessProbe(LivenessProbe livenessProbe) {
        this.livenessProbe = livenessProbe;
    }

    public List<OverrideSettings> getOverrideSettings() {
        return overrideSettings;
    }

    public void setOverrideSettings(List<OverrideSettings> overrideSettings) {
        this.overrideSettings = overrideSettings;
    }

    @NotNull
    public AgentType getAgentType() {
        return agentType;
    }

    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
    }

    public ConfigDto getConfig() {
        return config;
    }

    public void setConfig(ConfigDto config) {
        this.config = config;
    }

    public List<Volume> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<Volume> volumes) {
        this.volumes = volumes;
    }

    public List<EnvironmentVariable> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(List<EnvironmentVariable> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public ScaleSettings getScaleSettings() {
        return scaleSettings;
    }

    public void setScaleSettings(ScaleSettings scaleSettings) {
        this.scaleSettings = scaleSettings;
    }
}
