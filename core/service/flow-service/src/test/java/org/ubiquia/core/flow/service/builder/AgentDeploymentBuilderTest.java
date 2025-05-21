// package org.ubiquia.core.flow.service.builder;

// import java.time.Duration;
// import java.util.HashMap;
// import org.junit.jupiter.api.Assertions;
// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.TestPropertySource;
// import org.testcontainers.junit.jupiter.Container;
// import org.testcontainers.junit.jupiter.Testcontainers;
// import org.testcontainers.k3s.K3sContainer;
// import org.testcontainers.utility.DockerImageName;
// import org.ubiquia.core.flow.dummy.factory.DummyFactory;
// import org.ubiquia.core.flow.model.dto.ConfigDto;

// @SpringBootTest
// @Testcontainers
// @TestPropertySource(properties = {
//     "ubiquia.kubernetes.enabled=true"
// })
// public class AgentDeploymentBuilderTest {

//     static DockerImageName k3sImage = DockerImageName.parse("rancher/k3s:v1.24.10-k3s1");

//     @Autowired
//     private AgentDeploymentBuilder agentDeploymentBuilder;

//     @Container
//     static K3sContainer k3s = new K3sContainer(k3sImage)
//         .withStartupTimeout(Duration.ofMinutes(3));

//     @Autowired
//     private DummyFactory dummyFactory;

//     @BeforeAll
//     static void startCluster() {
//         k3s.start();
//     }


//     @Test
//     public void assertBuildsAgentConfigMap_IsValid() {

//         var config = new ConfigDto();
//         config.setConfigMountPath("/test/mount/path");

//         var map = new HashMap<String, String>();
//         map.put("test-key", "test-value");
//         config.setConfigMap(map);

//         var agent = this.dummyFactory.generateAgent();
//         agent.setConfig(config);

//         var configMap = this.agentDeploymentBuilder.tryBuildConfigMapFrom(agent);
//         var testName = agent.getAgentName().toLowerCase() + "-config";
//         Assertions.assertEquals(testName, configMap.getMetadata().getName());
//     }

//     @Test
//     public void assertBuildsAgentDeployment_IsValid() {

//         var agent = this.dummyFactory.generateAgent();
//         var testName = agent.getAgentName().toLowerCase() + "-config";
//         var deployment = this.agentDeploymentBuilder.buildDeploymentFrom(agent);
//         Assertions.assertEquals(testName, deployment.getMetadata().getName());
//     }

//     @Test
//     public void assertBuildsAgentService_IsValid() {

//         var agent = this.dummyFactory.generateAgent();
//         var testName = agent.getAgentName().toLowerCase() + "-config";
//         var service = this.agentDeploymentBuilder.buildServiceFrom(agent);
//         Assertions.assertEquals(testName, service.getMetadata().getName());
//     }
// }
