package org.ubiquia.core.flow.controller;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.library.dao.controller.GenericUbiquiaDaoController;
import org.ubiquia.common.library.implementation.service.mapper.GraphDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;
import org.ubiquia.core.flow.repository.ComponentRepository;
import org.ubiquia.core.flow.repository.GraphRepository;
import org.ubiquia.core.flow.repository.NodeRepository;
import org.ubiquia.core.flow.service.finder.AgentFinder;
import org.ubiquia.core.flow.service.finder.GraphFinder;
import org.ubiquia.core.flow.service.k8s.ComponentOperator;
import org.ubiquia.core.flow.service.manager.ComponentManager;
import org.ubiquia.core.flow.service.manager.NodeManager;
import org.ubiquia.core.flow.service.registrar.GraphRegistrar;

@RestController
@RequestMapping("/ubiquia/core/flow-service/graph")
public class GraphController extends GenericUbiquiaDaoController<GraphEntity, Graph> {

    private static final Logger logger = LoggerFactory.getLogger(GraphController.class);

    @Autowired
    private NodeManager nodeManager;

    @Autowired(required = false)
    private ComponentOperator componentOperator;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ComponentManager componentManager;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private EntityDao<GraphEntity> entityDao;

    @Autowired
    private GraphFinder graphFinder;

    @Autowired
    private GraphRegistrar graphRegistrar;

    @Autowired
    private GraphRepository graphRepository;

    @Autowired
    private GraphDtoMapper dtoMapper;

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentFinder agentFinder;

    @Autowired
    private AgentRepository agentRepository;

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public EntityDao<GraphEntity> getDataAccessObject() {
        return this.entityDao;
    }

    @Override
    public InterfaceEntityToDtoMapper<GraphEntity, Graph> getDataTransferObjectMapper() {
        return this.dtoMapper;
    }

    /**
     * Attempt to deploy a graph provided its name and version.
     *
     * @param deployment Data representing the graph to deploy.
     * @throws Exception Any exceptions from attempting to deploy the graph.
     */
    @PostMapping("/deploy")
    @Transactional
    public GraphDeployment tryDeployGraph(@RequestBody @Valid GraphDeployment deployment)
        throws Exception {

        var graphName = deployment.getGraphName();
        var domainOntologyName = deployment.getDomainOntologyName();
        var version = deployment.getDomainVersion();

        this.getLogger().info("Received a request to deploy graph "
                + "\nName: {} "
                + "\nOntology: {} "
                + "\nVersion {}...",
            graphName,
            domainOntologyName,
            version);

        var graphRecord = this
            .graphFinder
            .findDeployedGraphRecordWith(domainOntologyName, version, this.agentConfig.getId());

        if (graphRecord.isPresent()) {
            var json = this.objectMapper.writeValueAsString(deployment);
            throw new IllegalArgumentException("ERROR: Graph for Ubiquia Agent Id "
                + this.agentConfig.getId()
                + " is already deployed: "
                + json);
        }

        var ubiquiaAgentRecord = this.agentRepository.findById(this.agentConfig.getId());
        if (ubiquiaAgentRecord.isEmpty()) {
            throw new RuntimeException("ERROR: Could not find Ubiquia Agent with id "
                + this.agentConfig.getId()
                + "...");
        }

        var graphEntity = this
            .graphFinder
            .findGraphWith(graphName, domainOntologyName, version);

        var ubiquiaAgentEntity = ubiquiaAgentRecord.get();

        ubiquiaAgentEntity.getDeployedGraphs().add(graphEntity);
        this.agentRepository.save(ubiquiaAgentEntity);

        this.componentManager.tryDeployComponentsFor(deployment);
        this.nodeManager.tryDeployNodesFor(deployment);

        return deployment;
    }

    @PostMapping("/teardown")
    @Transactional
    public void tryTeardownGraph(@RequestBody @Valid GraphDeployment deployment) {

        this.getLogger().info("Received a request to teardown graph "
                + "\nName: {} "
                + "\nOntology: {} "
                + "\nversion {}...",
            deployment.getGraphName(),
            deployment.getDomainOntologyName(),
            deployment.getDomainVersion());

        this.nodeManager.tearDownNodesFor(deployment);

        if (Objects.nonNull(this.componentOperator)) {
            this.getLogger().info("...running in kubernetes; attempting to tear down any of the graph's "
                + " resources deployed in Kubernetes...");
            this.componentOperator.deleteGraphResources(deployment.getGraphName());
        }

        var ubiquiaAgentRecord = this.agentFinder.findAgentFor(deployment);

        if (ubiquiaAgentRecord.isEmpty()) {
            throw new RuntimeException("ERROR: Could not find a Ubiquia Agent for "
                + deployment.getGraphName()
                + " and semantic version "
                + deployment.getDomainVersion()
                + " and id "
                + this.agentConfig.getId());
        }

        var graphEntity = this.graphFinder.findGraphWith(
            deployment.getGraphName(),
            deployment.getDomainOntologyName(),
            deployment.getDomainVersion());

        var ubiquiaAgentEntity = ubiquiaAgentRecord.get();
        ubiquiaAgentEntity.getDeployedGraphs().remove(graphEntity);
        logger.info("...removing graph deployment from Ubiquia agent {}...",
            ubiquiaAgentEntity.getId());
        this.agentRepository.save(ubiquiaAgentEntity);

        this.getLogger().info("...finished tearing down the graph...");
    }
}
