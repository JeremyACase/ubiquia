package org.ubiquia.common.model.ubiquia.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import org.ubiquia.common.model.ubiquia.embeddable.*;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;

@Entity
public class ComponentEntity extends AbstractModelEntity {

    private ComponentType type;

    private CommunicationServiceSettings communicationServiceSettings;

    private Config config;

    private String name;

    private String description;

    private Boolean exposeService;

    private ScaleSettings scaleSettings;

    @ElementCollection
    @AttributeOverrides({
        @AttributeOverride(
            name = "name",
            column = @Column(name = "environment_variable_name")),
        @AttributeOverride(
            name = "value",
            column = @Column(name = "environment_variable_value", length = 512))
    })
    private Set<EnvironmentVariable> environmentVariables;

    private Image image;

    @ElementCollection
    private Set<OverrideSettingsStringified> overrideSettings;

    private Integer port;

    private InitContainer initContainer;

    private LivenessProbe livenessProbe;

    @OneToOne(mappedBy = "component", optional = true)
    private AdapterEntity adapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graph_component_join_id", nullable = false)
    private GraphEntity graph;

    @ElementCollection
    private Set<Volume> volumes;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NotNull
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public String getModelType() {
        return "Component";
    }

    public AdapterEntity getAdapter() {
        return adapter;
    }

    public void setAdapter(AdapterEntity adapterEntity) {
        this.adapter = adapterEntity;
    }

    public GraphEntity getGraph() {
        return graph;
    }

    public void setGraph(GraphEntity graphEntity) {
        this.graph = graphEntity;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
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

    public Set<OverrideSettingsStringified> getOverrideSettings() {
        return overrideSettings;
    }

    public void setOverrideSettings(Set<OverrideSettingsStringified> overrideSettings) {
        this.overrideSettings = overrideSettings;
    }

    public ComponentType getType() {
        return type;
    }

    public void setType(ComponentType type) {
        this.type = type;
    }

    public Set<Volume> getVolumes() {
        return volumes;
    }

    public void setVolumes(Set<Volume> volumes) {
        this.volumes = volumes;
    }

    public Set<EnvironmentVariable> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Set<EnvironmentVariable> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public ScaleSettings getScaleSettings() {
        return scaleSettings;
    }

    public void setScaleSettings(ScaleSettings scaleSettings) {
        this.scaleSettings = scaleSettings;
    }

    @NotNull
    public Boolean getExposeService() {
        return exposeService;
    }

    public void setExposeService(Boolean exposeService) {
        this.exposeService = exposeService;
    }

    @NotNull
    public CommunicationServiceSettings getCommunicationServiceSettings() {
        return communicationServiceSettings;
    }

    public void setCommunicationServiceSettings(CommunicationServiceSettings communicationServiceSettings) {
        this.communicationServiceSettings = communicationServiceSettings;
    }
}
