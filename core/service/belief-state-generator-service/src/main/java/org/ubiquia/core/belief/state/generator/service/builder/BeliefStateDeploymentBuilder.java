package org.ubiquia.core.belief.state.generator.service.builder;


import io.kubernetes.client.openapi.models.*;
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
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguage;

@ConditionalOnProperty(
    value = "ubiquia.kubernetes.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class BeliefStateDeploymentBuilder {

    private static final Logger logger = LoggerFactory.getLogger(BeliefStateDeploymentBuilder.class);
    @Value("${ubiquia.jdkVersion}")
    private String jdkVersion;
    @Value("${ubiquia.beliefStateGeneratorService.uber.jars.path}")
    private String uberJarsPath;
    @Autowired
    private BeliefStateNameBuilder beliefStateNameBuilder;
    private V1Deployment ubiquiaDeployment;

    public V1Deployment getUbiquiaDeployment() {
        return this.ubiquiaDeployment;
    }

    public void setUbiquiaDeployment(final V1Deployment ubiquiaDeployment) {
        this.ubiquiaDeployment = ubiquiaDeployment;
    }

    public V1Service buildServiceFrom(final AgentCommunicationLanguage acl) {
        var service = new V1Service();
        service.setApiVersion("v1");
        service.setKind("Service");

        service.setMetadata(new V1ObjectMeta());
        var beliefStateName = this.beliefStateNameBuilder.getKubernetesBeliefStateNameFrom(acl);
        service.getMetadata().setName(beliefStateName);

        service.getMetadata().setLabels(new HashMap<>());
        service.getMetadata().getLabels().putAll(this.ubiquiaDeployment.getMetadata().getLabels());
        service.getMetadata().getLabels().remove("component");
        service.getMetadata().getLabels().remove("app.kubernetes.io/managed-by");
        service.getMetadata().getLabels().put(
            "domain",
            acl.getDomain().toLowerCase());
        service.getMetadata().getLabels().put(
            "belief-state",
            beliefStateName);
        service.getMetadata().getLabels().put("app.kubernetes.io/managed-by", "ubiquia");

        var serviceSpec = new V1ServiceSpec();
        serviceSpec.setType("ClusterIP");
        serviceSpec.setSelector(new HashMap<>());
        serviceSpec.getSelector().put(
            "domain",
            acl.getDomain().toLowerCase());
        serviceSpec.getSelector().put(
            "belief-state",
            beliefStateName);
        service.setSpec(serviceSpec);

        serviceSpec.setPorts(new ArrayList<>());
        var port = new V1ServicePort();
        port.setProtocol("TCP");
        port.setPort(8080);
        port.setName("http");
        serviceSpec.getPorts().add(port);

        return service;
    }

    public V1Deployment buildDeploymentFrom(final AgentCommunicationLanguage acl) {

        var deployment = new V1Deployment();
        deployment.setApiVersion("apps/v1");
        deployment.setKind("Deployment");

        var metadata = this.getMetadataFrom(acl);
        deployment.setMetadata(metadata);

        var spec = this.getDeploymentSpecFrom(acl);
        deployment.setSpec(spec);

        return deployment;
    }

    private V1ObjectMeta getMetadataFrom(final AgentCommunicationLanguage acl) {
        var metadata = new V1ObjectMeta();

        var beliefStateName = this.beliefStateNameBuilder.getKubernetesBeliefStateNameFrom(acl);
        metadata.setName(beliefStateName);

        metadata.setLabels(new HashMap<>());
        metadata.getLabels().putAll(this.ubiquiaDeployment.getMetadata().getLabels());
        metadata.getLabels().remove("component");
        metadata.getLabels().remove("app.kubernetes.io/managed-by");
        metadata.getLabels().put("component", acl.getDomain().toLowerCase());
        metadata.getLabels().put("app.kubernetes.io/managed-by", "ubiquia");
        metadata.getLabels().put(
            "belief-state",
            beliefStateName);

        metadata.setAnnotations(new HashMap<>());
        metadata.getAnnotations().putAll(this.ubiquiaDeployment.getMetadata().getAnnotations());

        return metadata;
    }

    private V1DeploymentSpec getDeploymentSpecFrom(final AgentCommunicationLanguage acl) {

        var beliefStateName = this.beliefStateNameBuilder.getKubernetesBeliefStateNameFrom(acl);

        var spec = new V1DeploymentSpec();
        spec.setReplicas(1);
        spec.setSelector(this.ubiquiaDeployment.getSpec().getSelector());
        spec.getSelector().getMatchLabels().put(
            "domain",
            acl.getDomain().toLowerCase());
        spec.getSelector().getMatchLabels().put(
            "belief-state",
            beliefStateName);

        var template = this.getPodTemplateSpec(acl);
        spec.setTemplate(template);

        return spec;
    }

    private V1PodTemplateSpec getPodTemplateSpec(final AgentCommunicationLanguage acl) {

        var beliefStateName = this.beliefStateNameBuilder.getKubernetesBeliefStateNameFrom(acl);

        var template = new V1PodTemplateSpec();
        template.setMetadata(new V1ObjectMeta());
        template.getMetadata().setLabels(new HashMap<>());
        template.getMetadata().getLabels().putAll(this.ubiquiaDeployment.getMetadata().getLabels());
        template.getMetadata().getLabels().put("domain", acl.getDomain().toLowerCase());
        template.getMetadata().getLabels().put("belief-state", beliefStateName);
        template.getMetadata().getLabels().remove("app.kubernetes.io/managed-by");
        template.getMetadata().getLabels().put("app.kubernetes.io/managed-by", "ubiquia");

        var podSpec = new V1PodSpec();
        podSpec.setContainers(new ArrayList<>());
        podSpec.setImagePullSecrets(this
            .ubiquiaDeployment
            .getSpec()
            .getTemplate()
            .getSpec()
            .getImagePullSecrets());

        // Create container with updated volume mount
        var container = this.getContainer(acl);
        podSpec.getContainers().add(container);

        // PVC volume
        var jarVolume = new V1Volume()
            .name("belief-state-jar-volume")
            .persistentVolumeClaim(new V1PersistentVolumeClaimVolumeSource()
                .claimName("ubiquia-core-belief-state-generator-service-belief-state-jars-pvc"));
        podSpec.setVolumes(List.of(jarVolume));

        template.setSpec(podSpec);
        return template;
    }

    private V1Container getContainer(final AgentCommunicationLanguage acl) {

        var container = new V1Container();
        container.setName(acl.getDomain().toLowerCase());
        container.setImagePullPolicy("IfNotPresent");
        container.setImage("eclipse-temurin:" + this.jdkVersion);

        container.setEnv(List.of(
            new V1EnvVar()
                .name("MINIO_URL")
                .value("http://ubiquia-minio:9000"),
            new V1EnvVar()
                .name("MINIO_ROOT_USER")
                .valueFrom(new V1EnvVarSource()
                    .secretKeyRef(new V1SecretKeySelector()
                        .name("ubiquia-minio")
                        .key("root-user"))),
            new V1EnvVar()
                .name("MINIO_ROOT_PASSWORD")
                .valueFrom(new V1EnvVarSource()
                    .secretKeyRef(new V1SecretKeySelector()
                        .name("ubiquia-minio")
                        .key("root-password")))
        ));

        // Port setup
        var port = new V1ContainerPort()
            .containerPort(8080)
            .name("http")
            .protocol("TCP");
        container.setPorts(List.of(port));

        var beliefStateJarName = this.beliefStateNameBuilder.getJarBeliefStateNameFrom(acl);
        var mountPath = this.uberJarsPath + beliefStateJarName;

        // Volume mount with subPath
        container.setVolumeMounts(List.of(new V1VolumeMount()
            .name("belief-state-jar-volume")
            .mountPath(mountPath)
            .subPath(beliefStateJarName)));

        // Startup command
        container.setCommand(List.of("java"));
        container.setArgs(List.of("-jar", mountPath));

        return container;
    }
}
