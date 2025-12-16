package org.ubiquia.core.flow.service.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.models.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.ubiquia.common.model.ubiquia.dto.Config;
import org.ubiquia.core.flow.dummy.factory.DummyFactory;


@SpringBootTest(
    properties = {
        "ubiquia.kubernetes.enabled=true"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ComponentDeploymentBuilderTest {

    @Autowired
    private ComponentDeploymentBuilder componentDeploymentBuilder;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void assertBuildsConfigMap_isValid() {

        var data = Map.of(
            "application.yml",
            """
                config_0:
                  value: true
                config_1:
                  value: true
                """
        );

        var config = new Config();
        config.setConfigMap(data);
        config.setConfigMountPath("test");

        var component = this.dummyFactory.generateComponent();
        component.setConfig(config);

        var kubernetesConfigMap = this.componentDeploymentBuilder.tryBuildConfigMapFrom(component);

        Assertions.assertEquals(
            data.get("application.yml"),
            kubernetesConfigMap.getData().get("application.yml")
        );
    }

    @Test
    public void assertBuildsDeployment_isValid() {

        var baselineMeta = new V1ObjectMeta();
        baselineMeta.setLabels(new HashMap<>());
        baselineMeta.getLabels().put("app", "test");
        baselineMeta.getLabels().put("ubiquia", "true");
        baselineMeta.getLabels().put("component", "baseline"); // gets removed/replaced

        baselineMeta.setAnnotations(new HashMap<>());
        baselineMeta.getAnnotations().put("some-annotation", "some-value");

        var baselineSelector = new V1LabelSelector();
        baselineSelector.setMatchLabels(new HashMap<>());
        baselineSelector.getMatchLabels().put("app", "test");

        var baselinePodSpec = new V1PodSpec();
        baselinePodSpec.setImagePullSecrets(new ArrayList<>());

        var baselineTemplate = new V1PodTemplateSpec();
        baselineTemplate.setMetadata(new V1ObjectMeta());
        baselineTemplate.getMetadata().setLabels(new HashMap<>());
        baselineTemplate.getMetadata().getLabels().put("app", "test");
        baselineTemplate.setSpec(baselinePodSpec);

        var baselineSpec = new V1DeploymentSpec();
        baselineSpec.setSelector(baselineSelector);
        baselineSpec.setTemplate(baselineTemplate);

        var baselineDeployment = new V1Deployment();
        baselineDeployment.setMetadata(baselineMeta);
        baselineDeployment.setSpec(baselineSpec);

        ReflectionTestUtils.setField(
            componentDeploymentBuilder,
            "ubiquiaDeployment",
            baselineDeployment
        );

        var graph = this.dummyFactory.generateGraph();
        var component = this.dummyFactory.generateComponent();
        component.setEnvironmentVariables(new ArrayList<>());
        component.setGraph(graph);

        Assertions.assertDoesNotThrow(() ->
            this.componentDeploymentBuilder.buildDeploymentFrom(component)
        );
    }

    @Test
    public void assertBuildsService_isValid() {

        var deployment = new V1Deployment()
            .metadata(new V1ObjectMeta()
                .labels(Map.of(
                    "app", "test",
                    "ubiquia", "true"
                ))
            );

        ReflectionTestUtils.setField(
            componentDeploymentBuilder,
            "ubiquiaDeployment",
            deployment
        );

        var graph = this.dummyFactory.generateGraph();
        var component = this.dummyFactory.generateComponent();
        component.setGraph(graph);

        var kubernetesService = this.componentDeploymentBuilder.buildServiceFrom(component);

        Assertions.assertEquals(
            component.getName().toLowerCase(),
            kubernetesService.getMetadata().getName());
    }
}