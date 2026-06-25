package org.ubiquia.test.flow.service.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.config.FlowServiceConfig;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.dto.Component;
import org.ubiquia.common.model.ubiquia.dto.DomainDataContract;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.common.model.ubiquia.dto.FlowEvent;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.dto.GraphEdge;
import org.ubiquia.common.model.ubiquia.dto.Node;
import org.ubiquia.common.model.ubiquia.embeddable.EgressSettings;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.embeddable.GraphSettings;
import org.ubiquia.common.model.ubiquia.embeddable.Image;
import org.ubiquia.common.model.ubiquia.embeddable.NodeSettings;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;
import org.ubiquia.common.model.ubiquia.embeddable.SubSchema;
import org.ubiquia.common.model.ubiquia.enums.ComponentType;
import org.ubiquia.common.model.ubiquia.enums.NodeType;
import org.ubiquia.common.test.helm.service.AbstractHelmTestModule;

/**
 * Verifies that a source graph can forward data to a target graph via a node's targetGraph field.
 * The source graph uses a TEMPLATE component to generate dummy data that drives the flow.
 */
@Service
public class CrossGraphDataFlowTestModule extends AbstractHelmTestModule {

    private static final Logger logger =
        LoggerFactory.getLogger(CrossGraphDataFlowTestModule.class);

    private static final String ONTOLOGY_NAME = "cross-graph-test-ontology";
    private static final String SOURCE_GRAPH = "cross-graph-source";
    private static final String TARGET_GRAPH = "cross-graph-target";
    private static final String SOURCE_ENTRY_NODE = "cross-graph-source-entry";
    private static final String SOURCE_ROUTER_NODE = "cross-graph-source-router";
    private static final String TARGET_ENTRY_NODE = "cross-graph-target-entry";
    private static final String DOG_MODEL = "Dog";

    @Autowired
    private FlowServiceConfig flowServiceConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void doSetup() {
        logger.info("Registering cross-graph domain ontology...");
        try {
            var ontology = this.buildDomainOntology();
            var url = this.flowServiceConfig.getBaseUrl()
                + "/ubiquia/core/flow-service/domain-ontology/register/post";
            this.restTemplate.postForEntity(url, ontology, Object.class);
            logger.info("...cross-graph domain ontology registered.");
        } catch (Exception e) {
            this.testState.addFailure("ERROR registering domain ontology: " + e.getMessage());
        }
    }

    @Override
    public void doTests() {
        logger.info("Proceeding with cross-graph data flow tests...");

        this.deployGraph(SOURCE_GRAPH);
        this.deployGraph(TARGET_GRAPH);

        try {
            logger.info("...sleeping to allow graphs to initialize...");
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        this.pushDataToSourceGraph();

        try {
            logger.info("...sleeping to allow data to propagate to target graph...");
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        this.assertTargetGraphReceivedData();

        logger.info("...cross-graph data flow tests completed.");
    }

    @Override
    public void doCleanup() {
        logger.info("Tearing down cross-graph graphs...");
        this.teardownGraph(SOURCE_GRAPH);
        this.teardownGraph(TARGET_GRAPH);
        logger.info("...cross-graph graphs torn down.");
    }

    private void deployGraph(final String graphName) {
        var url = this.flowServiceConfig.getBaseUrl()
            + "/ubiquia/core/flow-service/graph/deploy";
        try {
            this.restTemplate.postForEntity(
                url, this.buildGraphDeployment(graphName), GraphDeployment.class);
            logger.info("...deployed graph: {}", graphName);
        } catch (Exception e) {
            this.testState.addFailure(
                "ERROR deploying graph " + graphName + ": " + e.getMessage());
        }
    }

    private void teardownGraph(final String graphName) {
        var url = this.flowServiceConfig.getBaseUrl()
            + "/ubiquia/core/flow-service/graph/teardown";
        try {
            this.restTemplate.postForEntity(
                url, this.buildGraphDeployment(graphName), GraphDeployment.class);
            logger.info("...torn down graph: {}", graphName);
        } catch (Exception e) {
            logger.warn("WARNING: Failed to tear down graph {}: {}", graphName, e.getMessage());
        }
    }

    private void pushDataToSourceGraph() {
        var url = this.flowServiceConfig.getBaseUrl()
            + "/ubiquia/core-flow-service/"
            + SOURCE_GRAPH
            + "/node/"
            + SOURCE_ENTRY_NODE
            + "/push";
        var payload = Map.of("name", "Fido", "breed", "Labrador");
        try {
            this.restTemplate.postForEntity(url, payload, Object.class);
            logger.info("...pushed Dog payload to source graph entry node.");
        } catch (Exception e) {
            this.testState.addFailure("ERROR pushing data to source graph: " + e.getMessage());
        }
    }

    private void assertTargetGraphReceivedData() {
        var url = this.flowServiceConfig.getBaseUrl()
            + "/ubiquia/core/flow-service/flow-event/query/params"
            + "?page=0&size=10&node.name=" + TARGET_ENTRY_NODE;
        try {
            var typeRef =
                new ParameterizedTypeReference<GenericPageImplementation<FlowEvent>>() {};
            var response = this.restTemplate.exchange(url, HttpMethod.GET, null, typeRef);
            var page = response.getBody();
            if (Objects.isNull(page) || page.getTotalElements() == 0) {
                this.testState.addFailure(
                    "No flow events found for target entry node '"
                        + TARGET_ENTRY_NODE
                        + "'. Expected at least one after source graph forwarded data"
                        + " via targetGraph.");
            } else {
                logger.info(
                    "Found {} flow event(s) at target entry node '{}'"
                        + " — cross-graph forwarding confirmed.",
                    page.getTotalElements(),
                    TARGET_ENTRY_NODE);
            }
        } catch (Exception e) {
            this.testState.addFailure(
                "ERROR querying flow events for target graph: " + e.getMessage());
        }
    }

    private GraphDeployment buildGraphDeployment(final String graphName) {
        var version = new SemanticVersion();
        version.setMajor(1);
        version.setMinor(0);
        version.setPatch(0);
        var deployment = new GraphDeployment();
        deployment.setGraphName(graphName);
        deployment.setDomainOntologyName(ONTOLOGY_NAME);
        deployment.setDomainVersion(version);
        deployment.setGraphSettings(new GraphSettings());
        return deployment;
    }

    private DomainOntology buildDomainOntology() {
        var version = new SemanticVersion();
        version.setMajor(1);
        version.setMinor(0);
        version.setPatch(0);

        var ontology = new DomainOntology();
        ontology.setName(ONTOLOGY_NAME);
        ontology.setAuthor("test");
        ontology.setDescription("Test ontology for cross-graph data flow verification.");
        ontology.setVersion(version);
        ontology.setDomainDataContract(this.buildDomainDataContract());

        var targetGraph = this.buildTargetGraph();
        var sourceGraph = this.buildSourceGraph(targetGraph);
        ontology.setGraphs(List.of(targetGraph, sourceGraph));

        return ontology;
    }

    private DomainDataContract buildDomainDataContract() {
        var dogProperties = new HashMap<String, Object>();
        dogProperties.put("name", Map.of("type", "string"));
        dogProperties.put("breed", Map.of("type", "string"));

        var dogSchema = new HashMap<String, Object>();
        dogSchema.put("type", "object");
        dogSchema.put("properties", dogProperties);

        var defs = new HashMap<String, Object>();
        defs.put(DOG_MODEL, dogSchema);

        var schema = new HashMap<String, Object>();
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        schema.put("$defs", defs);

        var contract = new DomainDataContract();
        contract.setSchema(schema);
        return contract;
    }

    private Graph buildTargetGraph() {
        var targetEntry = this.buildNode(
            TARGET_ENTRY_NODE, NodeType.HIDDEN, new ArrayList<>(), new ArrayList<>());

        var graph = new Graph();
        graph.setName(TARGET_GRAPH);
        graph.setAuthor("test");
        graph.setDescription("Target graph receiving forwarded data in cross-graph test.");
        graph.setNodes(new ArrayList<>(List.of(targetEntry)));
        graph.setComponents(new ArrayList<>());
        graph.setEdges(new ArrayList<>());
        graph.setCapabilities(new ArrayList<>());
        return graph;
    }

    private Graph buildSourceGraph(final Graph targetGraph) {
        var sourceRouter = this.buildNode(
            SOURCE_ROUTER_NODE, NodeType.HIDDEN,
            new ArrayList<>(List.of(SOURCE_ENTRY_NODE)), new ArrayList<>());
        sourceRouter.setTargetGraph(targetGraph);

        var outputSubSchema = new SubSchema();
        outputSubSchema.setModelName(DOG_MODEL);

        var sourceEntry = this.buildNode(
            SOURCE_ENTRY_NODE, NodeType.PUSH,
            new ArrayList<>(), new ArrayList<>(List.of(SOURCE_ROUTER_NODE)));
        sourceEntry.setOutputSubSchema(outputSubSchema);

        var edge = new GraphEdge();
        edge.setLeftNodeName(SOURCE_ENTRY_NODE);
        edge.setRightNodeNames(List.of(SOURCE_ROUTER_NODE));

        var templateComponent = this.buildTemplateComponent();

        var graph = new Graph();
        graph.setName(SOURCE_GRAPH);
        graph.setAuthor("test");
        graph.setDescription("Source graph forwarding data to target via targetGraph.");
        graph.setNodes(new ArrayList<>(List.of(sourceEntry, sourceRouter)));
        graph.setComponents(new ArrayList<>(List.of(templateComponent)));
        graph.setEdges(new ArrayList<>(List.of(edge)));
        graph.setCapabilities(new ArrayList<>());
        return graph;
    }

    private Node buildNode(
        final String name,
        final NodeType type,
        final List<String> upstreamNames,
        final List<String> downstreamNames) {

        var inputSubSchema = new SubSchema();
        inputSubSchema.setModelName(DOG_MODEL);

        var node = new Node();
        node.setName(name);
        node.setNodeType(type);
        node.setNodeSettings(new NodeSettings());
        node.setEgressSettings(new EgressSettings());
        node.setFlowEvents(new ArrayList<>());
        node.setInputSubSchemas(new ArrayList<>(List.of(inputSubSchema)));

        var upstreamNodes = new ArrayList<Node>();
        for (var upstreamName : upstreamNames) {
            var stub = new Node();
            stub.setName(upstreamName);
            upstreamNodes.add(stub);
        }
        node.setUpstreamNodes(upstreamNodes);

        var downstreamNodes = new ArrayList<Node>();
        for (var downstreamName : downstreamNames) {
            var stub = new Node();
            stub.setName(downstreamName);
            downstreamNodes.add(stub);
        }
        node.setDownstreamNodes(downstreamNodes);

        return node;
    }

    private Component buildTemplateComponent() {
        var image = new Image();
        image.setRegistry("localhost");
        image.setRepository("template-component");
        image.setTag("latest");

        var nodeRef = new Node();
        nodeRef.setName(SOURCE_ENTRY_NODE);

        var component = new Component();
        component.setName(SOURCE_ENTRY_NODE);
        component.setComponentType(ComponentType.TEMPLATE);
        component.setImage(image);
        component.setPort(8080);
        component.setDescription("TEMPLATE component for generating dummy Dog data.");
        component.setNode(nodeRef);

        return component;
    }
}
