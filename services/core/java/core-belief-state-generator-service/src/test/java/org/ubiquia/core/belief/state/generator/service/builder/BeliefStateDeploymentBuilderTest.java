package org.ubiquia.core.belief.state.generator.service.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.kubernetes.client.openapi.models.*;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.ubiquia.common.library.implementation.service.builder.BeliefStateNameBuilder;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
class BeliefStateDeploymentBuilderTest {

    private BeliefStateDeploymentBuilder builder;

    @Mock
    private BeliefStateNameBuilder beliefStateNameBuilder;

    @Mock
    private DomainOntology domainOntology;

    private static V1Deployment baseUbiquiaDeployment() {
        var deployment = new V1Deployment();

        var metadata = new V1ObjectMeta();
        metadata.setLabels(new HashMap<>());
        metadata.getLabels().put("app", "ubiquia");
        metadata.getLabels().put("component", "core");
        metadata.getLabels().put("app.kubernetes.io/managed-by", "helm");
        metadata.setAnnotations(new HashMap<>());
        metadata.getAnnotations().put("example-annotation", "example-value");
        deployment.setMetadata(metadata);

        var selector = new V1LabelSelector();
        selector.setMatchLabels(new HashMap<>());
        selector.getMatchLabels().put("app", "ubiquia");

        var spec = new V1DeploymentSpec();
        spec.setSelector(selector);

        var template = new V1PodTemplateSpec();
        var podSpec = new V1PodSpec();
        podSpec.setImagePullSecrets(
            List.of(new V1LocalObjectReference().name("my-registry-secret"))
        );
        template.setSpec(podSpec);

        spec.setTemplate(template);
        deployment.setSpec(spec);

        return deployment;
    }

    @BeforeEach
    void setUp() {
        builder = new BeliefStateDeploymentBuilder();

        ReflectionTestUtils.setField(builder, "beliefStateNameBuilder", beliefStateNameBuilder);
        ReflectionTestUtils.setField(builder, "jdkVersion", "21");
        ReflectionTestUtils.setField(builder, "uberJarsPath", "/opt/jars/");
        ReflectionTestUtils.setField(builder, "minioEnabled", Boolean.FALSE);

        builder.setUbiquiaDeployment(baseUbiquiaDeployment());
    }

    @Test
    void buildServiceFrom_setsClusterIpPortAndSelectorsAndLabels() {
        when(domainOntology.getName()).thenReturn("MyDomain");
        when(beliefStateNameBuilder.getKubernetesBeliefStateNameFrom(domainOntology))
            .thenReturn("bs-mydomain");

        var service = builder.buildServiceFrom(domainOntology);

        assertThat(service.getApiVersion()).isEqualTo("v1");
        assertThat(service.getKind()).isEqualTo("Service");
        assertThat(service.getMetadata().getName()).isEqualTo("bs-mydomain");

        var spec = service.getSpec();
        assertThat(spec.getType()).isEqualTo("ClusterIP");

        assertThat(spec.getPorts()).hasSize(1);
        var port = spec.getPorts().get(0);
        assertThat(port.getName()).isEqualTo("http");
        assertThat(port.getProtocol()).isEqualTo("TCP");
        assertThat(port.getPort()).isEqualTo(8080);

        assertThat(spec.getSelector())
            .containsEntry("domain", "mydomain")
            .containsEntry("belief-state", "bs-mydomain");

        assertThat(service.getMetadata().getLabels())
            .containsEntry("app", "ubiquia")
            .containsEntry("domain", "mydomain")
            .containsEntry("belief-state", "bs-mydomain")
            .containsEntry("app.kubernetes.io/managed-by", "ubiquia");

        assertThat(service.getMetadata().getLabels())
            .doesNotContainKey("component");
    }

    @Test
    void buildDeploymentFrom_setsMetadataNameLabelsAnnotationsAndSpec() {
        when(domainOntology.getName()).thenReturn("Finance");
        when(beliefStateNameBuilder.getKubernetesBeliefStateNameFrom(domainOntology))
            .thenReturn("bs-finance");

        var deployment = builder.buildDeploymentFrom(domainOntology);

        assertThat(deployment.getApiVersion()).isEqualTo("apps/v1");
        assertThat(deployment.getKind()).isEqualTo("Deployment");
        assertThat(deployment.getMetadata().getName()).isEqualTo("bs-finance");

        assertThat(deployment.getMetadata().getLabels())
            .containsEntry("app", "ubiquia")
            .containsEntry("component", "finance")
            .containsEntry("belief-state", "bs-finance")
            .containsEntry("app.kubernetes.io/managed-by", "ubiquia");

        assertThat(deployment.getMetadata().getAnnotations())
            .containsEntry("example-annotation", "example-value");

        var spec = deployment.getSpec();
        assertThat(spec.getReplicas()).isEqualTo(1);
        assertThat(spec.getSelector().getMatchLabels())
            .containsEntry("domain", "finance")
            .containsEntry("belief-state", "bs-finance");
    }

    @Test
    void buildDeploymentFrom_podTemplate_hasExpectedLabelsImagePullSecretsAndJarVolume() {
        when(domainOntology.getName()).thenReturn("Retail");
        when(beliefStateNameBuilder.getKubernetesBeliefStateNameFrom(domainOntology))
            .thenReturn("bs-retail");
        when(beliefStateNameBuilder.getJarBeliefStateNameFrom(domainOntology))
            .thenReturn("bs-retail.jar");

        var deployment = builder.buildDeploymentFrom(domainOntology);
        var template = deployment.getSpec().getTemplate();

        assertThat(template.getMetadata().getLabels())
            .containsEntry("app", "ubiquia")
            .containsEntry("domain", "retail")
            .containsEntry("belief-state", "bs-retail")
            .containsEntry("app.kubernetes.io/managed-by", "ubiquia");

        var podSpec = template.getSpec();

        assertThat(podSpec.getImagePullSecrets()).hasSize(1);
        assertThat(podSpec.getImagePullSecrets().get(0).getName())
            .isEqualTo("my-registry-secret");

        assertThat(podSpec.getVolumes()).hasSize(1);
        var volume = podSpec.getVolumes().get(0);
        assertThat(volume.getName()).isEqualTo("belief-state-jar-volume");
        assertThat(volume.getPersistentVolumeClaim().getClaimName())
            .isEqualTo("ubiquia-core-belief-state-generator-service-belief-state-jars-pvc");

        assertThat(podSpec.getContainers()).hasSize(1);
    }

    @Test
    void buildDeploymentFrom_container_hasExpectedImageCommandArgsPortAndVolumeMount() {
        when(domainOntology.getName()).thenReturn("Sales");
        when(beliefStateNameBuilder.getKubernetesBeliefStateNameFrom(domainOntology))
            .thenReturn("bs-sales");
        when(beliefStateNameBuilder.getJarBeliefStateNameFrom(domainOntology))
            .thenReturn("bs-sales.jar");

        var deployment = builder.buildDeploymentFrom(domainOntology);
        var container = deployment.getSpec()
            .getTemplate()
            .getSpec()
            .getContainers()
            .get(0);

        assertThat(container.getName()).isEqualTo("sales");
        assertThat(container.getImagePullPolicy()).isEqualTo("IfNotPresent");
        assertThat(container.getImage()).isEqualTo("eclipse-temurin:21");

        assertThat(container.getCommand()).containsExactly("java");
        assertThat(container.getArgs()).containsExactly("-jar", "/opt/jars/bs-sales.jar");

        assertThat(container.getPorts()).hasSize(1);
        var port = container.getPorts().get(0);
        assertThat(port.getContainerPort()).isEqualTo(8080);
        assertThat(port.getName()).isEqualTo("http");
        assertThat(port.getProtocol()).isEqualTo("TCP");

        assertThat(container.getVolumeMounts()).hasSize(1);
        var mount = container.getVolumeMounts().get(0);
        assertThat(mount.getName()).isEqualTo("belief-state-jar-volume");
        assertThat(mount.getMountPath()).isEqualTo("/opt/jars/bs-sales.jar");
        assertThat(mount.getSubPath()).isEqualTo("bs-sales.jar");
    }

    // ---------------------------------------------------------------------
    // Test helper
    // ---------------------------------------------------------------------

    @Test
    void buildDeploymentFrom_whenMinioEnabled_setsMinioEnvVars() {
        ReflectionTestUtils.setField(builder, "minioEnabled", Boolean.TRUE);

        when(domainOntology.getName()).thenReturn("Ops");
        when(beliefStateNameBuilder.getKubernetesBeliefStateNameFrom(domainOntology))
            .thenReturn("bs-ops");
        when(beliefStateNameBuilder.getJarBeliefStateNameFrom(domainOntology))
            .thenReturn("bs-ops.jar");

        var deployment = builder.buildDeploymentFrom(domainOntology);
        var container = deployment.getSpec()
            .getTemplate()
            .getSpec()
            .getContainers()
            .get(0);

        assertThat(container.getEnv()).hasSize(3);

        var minioUrl = container.getEnv().stream()
            .filter(e -> "MINIO_URL".equals(e.getName()))
            .findFirst()
            .orElseThrow();
        assertThat(minioUrl.getValue()).isEqualTo("http://ubiquia-minio:9000");

        var accessKey = container.getEnv().stream()
            .filter(e -> "MINIO_ACCESS_KEY".equals(e.getName()))
            .findFirst()
            .orElseThrow();
        assertThat(accessKey.getValueFrom().getSecretKeyRef().getName())
            .isEqualTo("ubiquia-minio");
        assertThat(accessKey.getValueFrom().getSecretKeyRef().getKey())
            .isEqualTo("root-user");

        var secretKey = container.getEnv().stream()
            .filter(e -> "MINIO_SECRET_KEY".equals(e.getName()))
            .findFirst()
            .orElseThrow();
        assertThat(secretKey.getValueFrom().getSecretKeyRef().getName())
            .isEqualTo("ubiquia-minio");
        assertThat(secretKey.getValueFrom().getSecretKeyRef().getKey())
            .isEqualTo("root-password");
    }
}
