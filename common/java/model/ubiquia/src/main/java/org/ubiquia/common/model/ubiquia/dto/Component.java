package org.ubiquia.common.model.ubiquia.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.ubiquia.common.model.ubiquia.embeddable.*;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;

public class Component extends AbstractModel {

    private ComponentType componentType;

    private Config config;

    private String name;

    private CommunicationServiceSettings communicationServiceSettings;

    private String description;

    private Boolean exposeService = false;

    private List<String> postStartExecCommands;

    private List<EnvironmentVariable> environmentVariables;

    private List<OverrideSettings> overrideSettings;

    private Integer port;

    private InitContainer initContainer;

    private LivenessProbe livenessProbe;

    private Adapter adapter;

    private Graph graph;

    private Image image;

    private List<Volume> volumes;

    private ScaleSettings scaleSettings;

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getModelType() {
        return "Component";
    }

    @NotNull
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @NotNull
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
    public ComponentType getComponentType() {
        return componentType;
    }

    public void setComponentType(ComponentType componentType) {
        this.componentType = componentType;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
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

    public Boolean getExposeService() {
        return exposeService;
    }

    public void setExposeService(Boolean exposeService) {
        this.exposeService = exposeService;
    }

    public CommunicationServiceSettings getCommunicationServiceSettings() {
        return communicationServiceSettings;
    }

    public void setCommunicationServiceSettings(CommunicationServiceSettings communicationServiceSettings) {
        this.communicationServiceSettings = communicationServiceSettings;
    }

    public List<String> getPostStartExecCommands() {
        return postStartExecCommands;
    }

    public void setPostStartExecCommands(List<String> postStartExecCommands) {
        this.postStartExecCommands = postStartExecCommands;
    }
}
