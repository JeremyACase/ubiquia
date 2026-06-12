package org.ubiquia.core.belief.state.generator.service.builder;

import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerPort;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1EnvVarSource;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimVolumeSource;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1SecretKeySelector;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.openapi.models.V1ServiceSpec;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.implementation.service.builder.BeliefStateNameBuilder;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;

/**
 * Builds Kubernetes {@link V1Service} and {@link V1Deployment} manifests for a
 * belief-state microservice derived from a {@link DomainOntology}.
 */
@ConditionalOnProperty(
    value = "ubiquia.kubernetes.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class BeliefStateDeploymentBuilder {

    private static final Logger logger =
        LoggerFactory.getLogger(BeliefStateDeploymentBuilder.class);

    @Value("${ubiquia.jdkVersion}")
    private String jdkVersion;

    @Value("${ubiquia.beliefStateGeneratorService.uber.jars.path}")
    private String uberJarsPath;

    @Value("${ubiquia.agent.storage.minio.enabled}")
    private Boolean minioEnabled;

    @Autowired
    private BeliefStateNameBuilder beliefStateNameBuilder;

    private V1Deployment ubiquiaDeployment;

    /**
     * Returns the Ubiquia deployment used as a template for label and spec inheritance.
     *
     * @return the base deployment
     */
    public V1Deployment getUbiquiaDeployment() {
        return this.ubiquiaDeployment;
    }

    /**
     * Sets the Ubiquia deployment template.
     *
     * @param ubiquiaDeployment the base deployment to inherit labels and spec from
     */
    public void setUbiquiaDeployment(final V1Deployment ubiquiaDeployment) {
        this.ubiquiaDeployment = ubiquiaDeployment;
    }

    /**
     * Builds a Kubernetes {@link V1Service} for the belief state derived from
     * {@code domainOntology}.
     *
     * @param domainOntology the domain ontology defining the belief state
     * @return the constructed service manifest
     */
    public V1Service buildServiceFrom(final DomainOntology domainOntology) {

        var beliefStateName =
            this.beliefStateNameBuilder.getKubernetesBeliefStateNameFrom(domainOntology);
        var domainName = domainOntology.getName().toLowerCase();

        var service = new V1Service();
        service.setApiVersion("v1");
        service.setKind("Service");

        service.setMetadata(new V1ObjectMeta());
        service.getMetadata().setName(beliefStateName);
        service.getMetadata().setLabels(
            K8sLabelBuilder.from(this.ubiquiaDeployment.getMetadata().getLabels())
                .remove("component", "app.kubernetes.io/managed-by")
                .put("domain", domainName)
                .withBeliefState(beliefStateName)
                .build());

        var serviceSpec = new V1ServiceSpec();
        serviceSpec.setType("ClusterIP");
        serviceSpec.setSelector(new HashMap<>());
        serviceSpec.getSelector().put("domain", domainName);
        serviceSpec.getSelector().put("belief-state", beliefStateName);
        service.setSpec(serviceSpec);

        serviceSpec.setPorts(new ArrayList<>());
        var port = new V1ServicePort();
        port.setProtocol("TCP");
        port.setPort(8080);
        port.setName("http");
        serviceSpec.getPorts().add(port);

        return service;
    }

    /**
     * Builds a Kubernetes {@link V1Deployment} for the belief state derived from
     * {@code domainOntology}.
     *
     * @param domainOntology the domain ontology defining the belief state
     * @return the constructed deployment manifest
     */
    public V1Deployment buildDeploymentFrom(final DomainOntology domainOntology) {

        var beliefStateName =
            this.beliefStateNameBuilder.getKubernetesBeliefStateNameFrom(domainOntology);
        var domainName = domainOntology.getName().toLowerCase();

        var deployment = new V1Deployment();
        deployment.setApiVersion("apps/v1");
        deployment.setKind("Deployment");
        deployment.setMetadata(this.buildDeploymentMetadata(beliefStateName, domainName));
        deployment.setSpec(this.buildDeploymentSpec(beliefStateName, domainName, domainOntology));

        return deployment;
    }

    private V1ObjectMeta buildDeploymentMetadata(
        final String beliefStateName,
        final String domainName) {

        var metadata = new V1ObjectMeta();
        metadata.setName(beliefStateName);
        metadata.setLabels(
            K8sLabelBuilder.from(this.ubiquiaDeployment.getMetadata().getLabels())
                .remove("component", "app.kubernetes.io/managed-by")
                .put("component", domainName)
                .withBeliefState(beliefStateName)
                .build());

        metadata.setAnnotations(new HashMap<>());
        metadata.getAnnotations().putAll(this.ubiquiaDeployment.getMetadata().getAnnotations());

        return metadata;
    }

    private V1DeploymentSpec buildDeploymentSpec(
        final String beliefStateName,
        final String domainName,
        final DomainOntology domainOntology) {

        var spec = new V1DeploymentSpec();
        spec.setReplicas(1);
        spec.setSelector(this.ubiquiaDeployment.getSpec().getSelector());
        spec.getSelector().getMatchLabels().put("domain", domainName);
        spec.getSelector().getMatchLabels().put("belief-state", beliefStateName);
        spec.setTemplate(this.buildPodTemplateSpec(beliefStateName, domainName, domainOntology));

        return spec;
    }

    private V1PodTemplateSpec buildPodTemplateSpec(
        final String beliefStateName,
        final String domainName,
        final DomainOntology domainOntology) {

        var template = new V1PodTemplateSpec();
        template.setMetadata(new V1ObjectMeta());
        template.getMetadata().setLabels(
            K8sLabelBuilder.from(this.ubiquiaDeployment.getMetadata().getLabels())
                .remove("app.kubernetes.io/managed-by")
                .put("domain", domainName)
                .withBeliefState(beliefStateName)
                .build());

        var podSpec = new V1PodSpec();
        podSpec.setContainers(new ArrayList<>());
        podSpec.setImagePullSecrets(this.ubiquiaDeployment
            .getSpec()
            .getTemplate()
            .getSpec()
            .getImagePullSecrets());

        podSpec.getContainers().add(this.buildContainer(beliefStateName, domainOntology));

        var jarVolume = new V1Volume()
            .name("belief-state-jar-volume")
            .persistentVolumeClaim(new V1PersistentVolumeClaimVolumeSource()
                // CHECKSTYLE.SUPPRESS: LineLength
                .claimName("ubiquia-core-belief-state-generator-service-belief-state-jars-pvc"));
        podSpec.setVolumes(List.of(jarVolume));

        template.setSpec(podSpec);
        return template;
    }

    private V1Container buildContainer(
        final String beliefStateName,
        final DomainOntology domainOntology) {

        var container = new V1Container();
        container.setName(domainOntology.getName().toLowerCase());
        container.setImagePullPolicy("IfNotPresent");
        container.setImage("eclipse-temurin:" + this.jdkVersion);

        if (this.minioEnabled) {
            container.setEnv(List.of(
                new V1EnvVar()
                    .name("MINIO_URL")
                    .value("http://ubiquia-minio:9000"),
                new V1EnvVar()
                    .name("MINIO_ACCESS_KEY")
                    .valueFrom(new V1EnvVarSource()
                        .secretKeyRef(new V1SecretKeySelector()
                            .name("ubiquia-minio")
                            .key("root-user"))),
                new V1EnvVar()
                    .name("MINIO_SECRET_KEY")
                    .valueFrom(new V1EnvVarSource()
                        .secretKeyRef(new V1SecretKeySelector()
                            .name("ubiquia-minio")
                            .key("root-password")))
            ));
        }

        var port = new V1ContainerPort()
            .containerPort(8080)
            .name("http")
            .protocol("TCP");
        container.setPorts(List.of(port));

        var jarName = this.beliefStateNameBuilder.getJarBeliefStateNameFrom(domainOntology);
        var mountPath = this.uberJarsPath + jarName;

        container.setVolumeMounts(List.of(new V1VolumeMount()
            .name("belief-state-jar-volume")
            .mountPath(mountPath)
            .subPath(jarName)));

        container.setCommand(List.of("java"));
        container.setArgs(List.of("-jar", mountPath));

        return container;
    }
}
