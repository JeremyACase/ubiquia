package org.ubiquia.core.flow.service.registrar;


import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.implementation.service.mapper.GraphDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.entity.ComponentEntity;
import org.ubiquia.common.model.ubiquia.entity.DomainOntologyEntity;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;
import org.ubiquia.common.model.ubiquia.entity.NodeEntity;
import org.ubiquia.core.flow.repository.ComponentRepository;
import org.ubiquia.core.flow.repository.DomainDataContractRepository;
import org.ubiquia.core.flow.repository.GraphRepository;
import org.ubiquia.core.flow.repository.NodeRepository;
import org.ubiquia.core.flow.service.manager.ComponentManager;
import org.ubiquia.core.flow.service.manager.NodeManager;
import org.ubiquia.core.flow.service.visitor.validator.GraphValidator;

/**
 * A service dedicated to registering graphs in Ubiquia.
 */
@Service
public class GraphRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(GraphRegistrar.class);
    @Autowired
    private NodeManager nodeManager;
    @Autowired
    private NodeRegistrar nodeRegistrar;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private ComponentRegistrar componentRegistrar;
    @Autowired
    private ComponentManager componentManager;
    @Autowired
    private ComponentRepository componentRepository;
    @Autowired
    private DomainDataContractRepository domainDataContractRepository;
    @Autowired
    private GraphRepository graphRepository;
    @Autowired
    private GraphDtoMapper graphDtoMapper;
    @Autowired
    private GraphValidator graphValidator;

    public void tryRegisterGraphs(
        DomainOntologyEntity domainOntologyEntity,
        final DomainOntology domainOntology)
        throws Exception {

        logger.info("Registering graphs for domain ontology: {}...",
            domainOntology.getName());

        for (var graph : domainOntology.getGraphs()) {
            this.tryRegisterGraph(domainOntologyEntity, graph);
        }

        logger.info("...registered graphs for domain ontology: {}.",
            domainOntology.getName());
    }

    /**
     * Attempt to register the provided graph with Ubiquia.
     *
     * @param graph The graph to register.
     * @return A newly-registered graph.
     * @throws Exception Exceptions from an invalid graph.
     */
    @Transactional
    public GraphEntity tryRegisterGraph(
        DomainOntologyEntity domainOntologyEntity,
        final Graph graph)
        throws Exception {

        logger.info("...registering graph {} for domain {}...",
            graph.getName(),
            graph.getDomainOntology().getName());

        this.tryCleanGraphRegistration(graph);

        var graphEntity = this.tryGetGraphEntityFrom(graph);
        graphEntity = this.persistGraph(domainOntologyEntity, graphEntity);

        var componentEntities = this
            .componentRegistrar
            .registerComponentsFor(graphEntity, graph);
        graphEntity.getComponents().addAll(componentEntities);

        var nodeEntities = this
            .nodeRegistrar
            .registerNodesFor(graphEntity, graph);
        graphEntity.getNodes().addAll(nodeEntities);

        this.tryAdaptComponentsToNodes(componentEntities, nodeEntities, graph);

        var dto = this.graphDtoMapper.map(graphEntity);
        this.graphValidator.tryValidate(dto);

        graphEntity = this.graphRepository.save(graphEntity);
        logger.info("...registered graph {}.", graph.getName());

        return graphEntity;
    }

    @Transactional
    private void tryAdaptComponentsToNodes(
        List<ComponentEntity> componentEntities,
        List<NodeEntity> nodeEntities,
        final Graph graph) {

        logger.info("...attempting to adapt nodes to components...");

        for (var component : graph.getComponents()) {
            if (Objects.nonNull(component.getNode())) {

                var nodeRecord = nodeEntities
                    .stream()
                    .filter(x -> x
                        .getName()
                        .equals(component.getNode().getName()))
                    .findFirst();

                if (nodeRecord.isEmpty()) {
                    throw new IllegalArgumentException("ERROR: Could not find node named: "
                        + component.getNode().getName());
                }
                var nodeEntity = nodeRecord.get();

                logger.info("...component named {} is adapted to node {}; connecting...",
                    component.getName(),
                    nodeEntity.getName());

                if (Objects.nonNull(nodeEntity.getComponent())) {
                    throw new IllegalArgumentException("ERROR: Node is already adapted to "
                        + "component named: "
                        + nodeEntity.getComponent().getNode());
                }

                var componentRecord = componentEntities
                    .stream()
                    .filter(x -> x
                        .getName()
                        .equals(component.getName()))
                    .findFirst();

                if (componentRecord.isEmpty()) {
                    throw new IllegalArgumentException("ERROR: Could not find component named: "
                        + component.getNode().getName());
                }
                var componentEntity = componentRecord.get();

                nodeEntity.setComponent(componentEntity);
                nodeEntity = this.nodeRepository.save(nodeEntity);
                componentEntity.setNode(nodeEntity);
                this.componentRepository.save(componentEntity);
                logger.info("...completed adapting node to component.");
            }
        }

        logger.info("...completed adapting nodes to components.");
    }

    /**
     * Attempt to clean a graph by setting various fields as necessary.
     *
     * @param graph The graph object to clean.
     */
    private void tryCleanGraphRegistration(Graph graph) {

        if (Objects.isNull(graph.getEdges())) {
            graph.setEdges(new ArrayList<>());
        }

        if (Objects.isNull(graph.getCapabilities())) {
            graph.setCapabilities(new ArrayList<>());
        }

        if (Objects.isNull(graph.getComponents())) {
            graph.setComponents(new ArrayList<>());
        }

        if (Objects.isNull(graph.getNodes())) {
            graph.setNodes(new ArrayList<>());
        }

        if (Objects.isNull(graph.getTags())) {
            graph.setTags(new ArrayList<>());
        }
    }

    @Transactional
    private GraphEntity persistGraph(
        DomainOntologyEntity domainOntologyEntity,
        GraphEntity graphEntity) {

        graphEntity.setDomainOntology(domainOntologyEntity);
        graphEntity = this.graphRepository.save(graphEntity);

        return graphEntity;
    }

    /**
     * Provided a graph registration object, attempt to get an equivalent entity.
     *
     * @param graph The graph registration object.
     * @return A graph entity.
     */
    private GraphEntity tryGetGraphEntityFrom(final Graph graph) {

        var graphEntity = new GraphEntity();
        graphEntity.setAuthor(graph.getAuthor());
        graphEntity.setCapabilities(graph.getCapabilities());
        graphEntity.setDescription(graph.getDescription());
        graphEntity.setName(graph.getName());
        graphEntity.setComponents(new HashSet<>());
        graphEntity.setNodes(new HashSet<>());
        graphEntity.setAgentsDeployingGraph(new ArrayList<>());

        graphEntity.setTags(new HashSet<>());
        if (Objects.nonNull(graph.getTags())) {
            graphEntity.getTags().addAll(graph.getTags());
        }

        return graphEntity;
    }
}
