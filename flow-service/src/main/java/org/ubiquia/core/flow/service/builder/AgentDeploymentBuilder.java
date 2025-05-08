package org.ubiquia.core.flow.service.builder;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import org.apache.logging.log4j.util.Strings;
import org.machina.core.amigos.model.dto.DataTransformDTO;
import org.machina.core.amigos.model.embeddable.Volume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.models.*;
import org.ubiquia.core.flow.model.dto.AgentDto;

@ConditionalOnProperty(
    value = "amigos.kubernetes.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class AgentDeploymentBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AgentDeploymentBuilder.class);
    private V1Deployment ubiquiaDeployment;
    @Autowired
    private ObjectMapper objectMapper;

    public V1Deployment getUbiquiaDeployment() {
        return this.ubiquiaDeployment;
    }

    public void setUbiquiaDeployment(final V1Deployment ubiquiaDeployment) {
        this.ubiquiaDeployment = ubiquiaDeployment;
    }

    /**
     * If applicable, try to get a Kubernetes configmap for an agent.
     *
     * @param agent The agent to get a configmap for.
     * @return A Configmap if applicable, or null.
     */
    public V1ConfigMap tryBuildConfigMapFrom(final AgentDto agent) {
        V1ConfigMap configMap = null;

        if (Objects.nonNull(agent.getConfig())) {
            configMap = new V1ConfigMap();
            var typeRef = new TypeReference<HashMap<String, String>>() {};
            var map = this.objectMapper.convertValue(
                agent.getConfig().getConfigMap(),
                typeRef);
            configMap.setData(map);

            configMap.setMetadata(new V1ObjectMeta());
            configMap.getMetadata().setName(
                agent.getAgentName().toLowerCase() + "-config");
        }
        return configMap;
    }

    /**
     * Attempt to build a Kubernetes service for the data transform.
     *
     * @param agent The data transform to build a service for.
     * @return A Kubernetes service.
     */
    @Transactional
    public V1Service buildServiceFrom(final AgentDto agent) {
        var service = new V1Service();
        service.setApiVersion("v1");
        service.setKind("Service");

        service.setMetadata(new V1ObjectMeta());
        service.getMetadata().setName(agent.getAgentName().toLowerCase());

        service.getMetadata().setLabels(new HashMap<>());
        service.getMetadata().getLabels().putAll(this.ubiquiaDeployment.getMetadata().getLabels());
        service.getMetadata().getLabels().remove("component");
        service.getMetadata().getLabels().remove("app.kubernetes.io/managed-by");
        service.getMetadata().getLabels().put(
            "component",
            agent.getAgentName().toLowerCase());
        service.getMetadata().getLabels().put(
            "amigos-graph",
            agent.getGraph().getGraphName().toLowerCase());
        service.getMetadata().getLabels().put("app.kubernetes.io/managed-by", "amigos");

        var serviceSpec = new V1ServiceSpec();
        serviceSpec.setType("ClusterIP");
        serviceSpec.setSelector(new HashMap<>());
        serviceSpec.getSelector().put(
            "component",
            agent.getAgentName().toLowerCase());
        serviceSpec.getSelector().put(
            "amigos-graph",
            agent.getGraph().getGraphName().toLowerCase());
        service.setSpec(serviceSpec);

        serviceSpec.setPorts(new ArrayList<>());
        var port = new V1ServicePort();
        port.setProtocol("TCP");
        port.setPort(agent.getPort());
        port.setName("http");
        serviceSpec.getPorts().add(port);

        return service;
    }

    /**
     * Attempt to build a Kubernetes deployment for the data transform.
     *
     * @param dataTransform The data transform to build a deployment for.
     * @return A Kubernetes deployment.
     */
    @Transactional
    public V1Deployment buildDeploymentFrom(final DataTransformDTO dataTransform) {
        var deployment = new V1Deployment();
        deployment.setApiVersion("apps/v1");
        deployment.setKind("Deployment");
        deployment.setMetadata(this.getMetadataFrom(dataTransform));
        deployment.setSpec(this.getDeploymentSpecFrom(dataTransform));
        return deployment;
    }

    /**
     * Attempt to build a Kubernetes metadata for the data transform.
     *
     * @param dataTransform The data transform to build a metadata for.
     * @return A Kubernetes metadata.
     */
    @Transactional
    private V1ObjectMeta getMetadataFrom(final DataTransformDTO dataTransform) {
        var metadata = new V1ObjectMeta();
        metadata.setName(dataTransform.getDataTransformName().toLowerCase());

        metadata.setLabels(new HashMap<>());
        metadata.getLabels().putAll(this.ubiquiaDeployment.getMetadata().getLabels());
        metadata.getLabels().remove("component");
        metadata.getLabels().remove("app.kubernetes.io/managed-by");
        metadata.getLabels().put("component", dataTransform.getDataTransformName().toLowerCase());
        metadata.getLabels().put("app.kubernetes.io/managed-by", "amigos");
        metadata.getLabels().put(
            "amigos-graph",
            dataTransform.getGraph().getGraphName().toLowerCase());

        metadata.setAnnotations(new HashMap<>());
        metadata.getAnnotations().putAll(this.ubiquiaDeployment.getMetadata().getAnnotations());

        return metadata;
    }

    /**
     * Attempt to build a Kubernetes deployment spec for the data transform.
     *
     * @param dataTransform The data transform to build a deployment spec for.
     * @return A Kubernetes deployment spec.
     */
    @Transactional
    private V1DeploymentSpec getDeploymentSpecFrom(final DataTransformDTO dataTransform) {
        var spec = new V1DeploymentSpec();
        spec.setReplicas(dataTransform.getScaleSettings().getMinReplicas());
        spec.setSelector(this.ubiquiaDeployment.getSpec().getSelector());
        spec.getSelector().getMatchLabels().put(
            "component",
            dataTransform.getDataTransformName().toLowerCase());
        spec.getSelector().getMatchLabels().put(
            "amigos-graph",
            dataTransform.getGraph().getGraphName().toLowerCase());

        spec.setTemplate(this.getPodTemplateSpec(dataTransform));

        return spec;
    }

    /**
     * Attempt to build a Kubernetes template spec for the data transform.
     *
     * @param dataTransform The data transform to build a template spec for.
     * @return A Kubernetes template spec.
     */
    @Transactional
    private V1PodTemplateSpec getPodTemplateSpec(final DataTransformDTO dataTransform) {

        var template = new V1PodTemplateSpec();
        template.setMetadata(new V1ObjectMeta());
        template.getMetadata().setLabels(new HashMap<>());
        template.getMetadata().getLabels().putAll(this.ubiquiaDeployment.getMetadata().getLabels());
        template.getMetadata().getLabels().put(
            "component",
            dataTransform.getDataTransformName().toLowerCase());
        template.getMetadata().getLabels().put(
            "amigos-graph",
            dataTransform.getGraph().getGraphName().toLowerCase());
        template.getMetadata().getLabels().remove("app.kubernetes.io/managed-by");
        template.getMetadata().getLabels().put("app.kubernetes.io/managed-by", "amigos");

        template.setSpec(new V1PodSpec());
        template.setSpec(this.ubiquiaDeployment.getSpec().getTemplate().getSpec());
        template.getSpec().setContainers(new ArrayList<>());
        template.getSpec().getContainers().add(this.getContainer(dataTransform));
        template.getSpec().setImagePullSecrets(
            this.ubiquiaDeployment
                .getSpec()
                .getTemplate()
                .getSpec()
                .getImagePullSecrets());

        template.getSpec().setVolumes(new ArrayList<>());
        if (Objects.nonNull(dataTransform.getConfig())) {
            var volume = new V1Volume();
            volume.setName(dataTransform.getDataTransformName().toLowerCase()
                + "-config");
            volume.setConfigMap(new V1ConfigMapVolumeSource());
            volume.getConfigMap().setName(
                dataTransform.getDataTransformName().toLowerCase()
                    + "-config");

            template.getSpec().getVolumes().add(volume);
        }

        for (var volume : dataTransform.getVolumes()) {
            var volumeResource = this.tryGetVolume(volume);
            template.getSpec().addVolumesItem(volumeResource);
        }

        template.getSpec().setInitContainers(new ArrayList<>());
        var initContainer = this.tryGetInitContainer(dataTransform);
        if (Objects.nonNull(initContainer)) {
            template.getSpec().getInitContainers().add(initContainer);
        }

        return template;
    }

    /**
     * Attempt to build a Kubernetes container for the data transform.
     *
     * @param dataTransform The data transform to build a container for.
     * @return A Kubernetes container.
     */
    @Transactional
    private V1Container getContainer(final DataTransformDTO dataTransform) {
        var container = new V1Container();
        container.setName(dataTransform.getDataTransformName().toLowerCase());
        container.setImagePullPolicy("IfNotPresent");

        var image = "";

        if (!Strings.isEmpty(dataTransform.getImage().getRegistry())) {
            image += dataTransform.getImage().getRegistry();
            image += "/";
        }

        image = image
            + dataTransform.getImage().getRepository()
            + ":"
            + dataTransform.getImage().getTag();

        container.setImage(image);

        container.setPorts(new ArrayList<>());
        var port = new V1ContainerPort();
        port.setContainerPort(dataTransform.getPort());
        port.setName("http");
        port.setProtocol("TCP");
        port.setHostIP("http");
        container.getPorts().add(port);

        if (Objects.nonNull(dataTransform.getConfig())) {
            var envFrom = new V1EnvFromSource();
            envFrom.setConfigMapRef(new V1ConfigMapEnvSource());
            envFrom.getConfigMapRef().setName(dataTransform.getDataTransformName().toLowerCase()
                + "-config");

            container.setEnvFrom(new ArrayList<>());
            container.getEnvFrom().add(envFrom);
        }

        if (Objects.nonNull(dataTransform.getLivenessProbe())) {
            var probe = this.tryGetLivenessProbe(dataTransform);
            container.setLivenessProbe(probe);
        }

        container.setVolumeMounts(new ArrayList<>());
        for (var volume : dataTransform.getVolumes()) {
            var mount = this.tryGetVolumeMount(volume);
            container.getVolumeMounts().add(mount);
        }

        container.setEnv(new ArrayList<>());
        for (var envVar : dataTransform.getEnvironmentVariables()) {
            var environmentVariable = new V1EnvVar();
            environmentVariable.setName(envVar.getName());
            environmentVariable.setValue(envVar.getValue());
            container.getEnv().add(environmentVariable);
        }

        return container;
    }

    /**
     * Attempt to build a liveness probe for the data transform.
     *
     * @param dataTransform The data transform to build a volume mount for.
     * @return A livess probe.
     */
    private V1Probe tryGetLivenessProbe(final DataTransformDTO dataTransform) {
        V1Probe probe = null;
        if (Objects.nonNull(dataTransform.getLivenessProbe())) {
            probe = new V1Probe();
            probe.setInitialDelaySeconds(dataTransform.getLivenessProbe()
                .getInitialDelaySeconds());
            var httpGet = new V1HTTPGetAction();
            httpGet.setPath(dataTransform.getLivenessProbe().getHttpGetPath());
            httpGet.setPort(new IntOrString(dataTransform.getPort()));
            probe.setHttpGet(httpGet);
        }
        return probe;
    }

    /**
     * Attempt to build a Kubernetes init container for the data transform.
     *
     * @param dataTransform The data transform to build an init container for.
     * @return An init container.
     */
    private V1Container tryGetInitContainer(final DataTransformDTO dataTransform) {
        V1Container initContainer = null;
        if (Objects.nonNull(dataTransform.getInitContainer())) {
            initContainer = new V1Container();
            initContainer.setName(dataTransform.getDataTransformName().toLowerCase()
                + "-init-container");
            initContainer.setImagePullPolicy("IfNotPresent");

            initContainer.setCommand(dataTransform.getInitContainer().getCommand());
            initContainer.setArgs(dataTransform.getInitContainer().getArgs());
            initContainer.setImage(dataTransform.getInitContainer().getImage());
        }
        return initContainer;
    }

    /**
     * Attempt to build a Kubernetes volume for the amigos volume.
     *
     * @param volume The amigos volume to build a k8s volume for.
     * @return A volume.
     */
    private V1Volume tryGetVolume(final Volume volume) {
        var volumeResource = new V1Volume();
        volumeResource.setName(volume.getName());
        volumeResource.setPersistentVolumeClaim(new V1PersistentVolumeClaimVolumeSource());
        volumeResource.getPersistentVolumeClaim().setClaimName(volume.getPersistentVolumeClaimName());
        return volumeResource;
    }

    /**
     * Attempt to build a volume mount for the data transform.
     *
     * @param volume The volume we're building a mount for.
     * @return A volume mount.
     */
    private V1VolumeMount tryGetVolumeMount(final Volume volume) {
        var volumeMount = new V1VolumeMount();
        volumeMount.setName(volume.getName());
        volumeMount.setMountPath(volume.getMountPath());
        return volumeMount;
    }
}
