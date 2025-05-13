package org.ubiquia.core.flow.service.registrar;


import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.model.dto.GraphDto;
import org.ubiquia.core.flow.model.entity.AgentCommunicationLanguage;
import org.ubiquia.core.flow.model.entity.Graph;
import org.ubiquia.core.flow.repository.AgentCommunicationLanguageRepository;
import org.ubiquia.core.flow.repository.GraphRepository;
import org.ubiquia.core.flow.service.visitor.validator.GraphValidator;
import org.ubiquia.core.flow.service.manager.AdapterManager;
import org.ubiquia.core.flow.service.manager.AgentManager;

/**
 * A service dedicated to registering graphs in AMIGOS.
 */
@Service
public class GraphRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(GraphRegistrar.class);
    @Autowired
    private AdapterManager adapterManager;
    @Autowired
    private AdapterRegistrar adapterRegistrar;
    @Autowired
    private AgentRegistrar agentRegistrar;
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private AgentCommunicationLanguageRepository agentCommunicationLanguageRepository;
    @Autowired
    private GraphRepository graphRepository;
    @Autowired
    private GraphValidator graphValidator;

    /**
     * Attempt to register the provided graph with AMIGOS.
     *
     * @param graphRegistration The graph to register.
     * @return A newly-registered graph.
     * @throws Exception Exceptions from an invalid graph.
     */
    @Transactional
    public Graph tryRegister(final GraphDto graphRegistration) throws Exception {

        logger.info("...registering graph {} with version {} for domain {}...",
            graphRegistration.getGraphName(),
            graphRegistration.getVersion(),
            graphRegistration.getAgentCommunicationLanguage().getName());

        this.cleanGraphRegistration(graphRegistration);
        var domainOntologyEntity = this.tryGetAgentCommunicationLanguageFrom(graphRegistration);
        var graphEntity = this.tryGetGraphEntityFrom(graphRegistration);
        graphEntity = this.persistGraph(domainOntologyEntity, graphEntity);

        var agentEntities = this.agentRegistrar.registerAgentsFor(
            graphEntity,
            graphRegistration);
        graphEntity.setAgents(agentEntities);

        var adapterEntities = this.adapterRegistrar.registerAdaptersFor(
            graphEntity,
            graphRegistration);
        graphEntity.setAdapters(adapterEntities);
        this.graphValidator.tryValidate(graphEntity, graphRegistration);

        graphEntity = this.graphRepository.save(graphEntity);
        logger.info("...registered graph {}.", graphRegistration.getGraphName());

        return graphEntity;
    }

    /**
     * Attempt to clean a graph by setting various fields as necessary.
     *
     * @param graph The graph object to clean.
     */
    private void cleanGraphRegistration(GraphDto graph) {

        if (Objects.isNull(graph.getAgentlessAdapters())) {
            graph.setAgentlessAdapters(new ArrayList<>());
        }

        if (Objects.isNull(graph.getEdges())) {
            graph.setEdges(new ArrayList<>());
        }

        if (Objects.isNull(graph.getCapabilities())) {
            graph.setCapabilities(new ArrayList<>());
        }

        if (Objects.isNull(graph.getAgents())) {
            graph.setAgents(new ArrayList<>());
        }

        if (Objects.isNull(graph.getTags())) {
            graph.setTags(new ArrayList<>());
        }
    }

    /**
     * Attempt to persist a graph entity for a given domain ontology.
     *
     * @param agentCommunicationLanguageEntity The domain ontology to associate the graph with.
     * @param graphEntity          The graph to register.
     * @return The newly-persisted graph.
     */
    @Transactional
    private Graph persistGraph(
        AgentCommunicationLanguage agentCommunicationLanguageEntity,
        Graph graphEntity) {

        if (Objects.isNull(agentCommunicationLanguageEntity.getGraphs())) {
            agentCommunicationLanguageEntity.setGraphs(new ArrayList<>());
        }
        agentCommunicationLanguageEntity.getGraphs().add(graphEntity);
        agentCommunicationLanguageEntity = this
            .agentCommunicationLanguageRepository
            .save(agentCommunicationLanguageEntity);

        graphEntity.setAgentCommunicationLanguage(agentCommunicationLanguageEntity);
        graphEntity = this.graphRepository.save(graphEntity);
        return graphEntity;
    }

    /**
     * Provided a graph registration object, attempt to get an equivalent entity.
     *
     * @param graphRegistration The graph registration object.
     * @return A graph entity.
     */
    @Transactional
    private Graph tryGetGraphEntityFrom(final GraphDto graphRegistration) {

        var record = this.graphRepository
            .findByGraphNameAndVersionMajorAndVersionMinorAndVersionPatch(
                graphRegistration.getGraphName(),
                graphRegistration.getVersion().getMajor(),
                graphRegistration.getVersion().getMinor(),
                graphRegistration.getVersion().getPatch()
            );

        if (record.isPresent()) {
            throw new IllegalArgumentException("ERROR: A graph with name "
                + graphRegistration.getGraphName()
                + " and version "
                + graphRegistration.getVersion()
                + " has already been registered!");
        }

        var graphEntity = new Graph();
        graphEntity.setAuthor(graphRegistration.getAuthor());
        graphEntity.setCapabilities(graphRegistration.getCapabilities());
        graphEntity.setDescription(graphRegistration.getDescription());
        graphEntity.setGraphName(graphRegistration.getGraphName());
        graphEntity.setVersion(graphRegistration.getVersion());
        graphEntity.setAgents(new ArrayList<>());
        graphEntity.setAdapters(new ArrayList<>());

        graphEntity.setTags(new HashSet<>());
        if (Objects.nonNull(graphRegistration.getTags())) {
            graphEntity.getTags().addAll(graphRegistration.getTags());
        }

        return graphEntity;
    }

    /**
     * A helper method that can retrieve a Domain Ontology for a graph - if one exists.
     *
     * @param graphRegistration The graph to get an ontology for.
     * @return The graph's ontology.
     */
    @Transactional
    private AgentCommunicationLanguage tryGetAgentCommunicationLanguageFrom(
        final GraphDto graphRegistration) {

        var acl = graphRegistration.getAgentCommunicationLanguage();
        if (Objects.isNull(acl)) {
            throw new IllegalArgumentException("ERROR: Cannot register a graph "
                + " with a NULL domain ontology!");
        }

        var record = this
            .agentCommunicationLanguageRepository
            .findByDomainAndVersionMajorAndVersionMinorAndVersionPatch(
                acl.getName(),
                acl.getVersion().getMajor(),
                acl.getVersion().getMinor(),
                acl.getVersion().getPatch());

        if (record.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Cannot find an agent communication language "
                + " registered with the name: "
                + acl.getName()
                + " and version: "
                + graphRegistration.getVersion());
        }

        return record.get();
    }
}
