package org.ubiquia.core.flow.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.core.flow.model.embeddable.*;
import org.ubiquia.core.flow.model.enums.AgentType;

@Validated
public class AgentDto extends AbstractEntityDto {

    @JsonProperty("dataTransformType")
    private AgentType agentType;

    @JsonProperty("config")
    private ConfigDTO config;

    @JsonProperty("agentName")
    private String agentName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("environmentVariables")
    private List<EnvironmentVariable> environmentVariables;

    private List<OverrideSettings> overrideSettings;

    @JsonProperty("port")
    private Integer port;

    @JsonProperty("initContainer")
    private InitContainer initContainer;

    @JsonProperty("livenessProbe")
    private LivenessProbe livenessProbe;

    @JsonProperty("adapter")
    private AdapterDto adapter;

    @JsonProperty("graph")
    private GraphDto graph;

    @JsonProperty("image")
    private Image image;

    @JsonProperty("volumes")
    private List<Volume> volumes;

    @JsonProperty("scaleSettings")
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
    public AgentType getDataTransformType() {
        return agentType;
    }

    public void setDataTransformType(AgentType agentType) {
        this.agentType = agentType;
    }

    public ConfigDTO getConfig() {
        return config;
    }

    public void setConfig(ConfigDTO config) {
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
