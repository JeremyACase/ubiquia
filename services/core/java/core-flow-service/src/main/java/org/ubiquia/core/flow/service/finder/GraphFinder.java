package org.ubiquia.core.flow.service.finder;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;
import org.ubiquia.core.flow.repository.GraphRepository;

@Service
public class GraphFinder {
    private static final Logger logger = LoggerFactory.getLogger(GraphFinder.class);

    @Autowired
    private GraphRepository graphRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AgentConfig agentConfig;

    @Transactional
    public GraphEntity findGraphWith(
        final String graphName,
        final String domainOntologyName,
        final SemanticVersion domainOntologyVersion) {

        var graphRecord = this
            .graphRepository
            .findByNameAndDomainOntologyNameAndDomainOntologyVersionMajorAndDomainOntologyVersionMinorAndDomainOntologyVersionPatch(
                graphName,
                domainOntologyName,
                domainOntologyVersion.getMajor(),
                domainOntologyVersion.getMinor(),
                domainOntologyVersion.getPatch());

        if (graphRecord.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Could not find a graph named "
                + graphName);
        }

        return graphRecord.get();
    }

    @Transactional
    public Optional<GraphEntity> findDeployedGraphRecordWith(
        final String domainOntologyName,
        final SemanticVersion domainOntologyVersion,
        final String agentId) {

        var graphRecord = this
            .graphRepository
            .findByDomainOntologyNameAndDomainOntologyVersionMajorAndDomainOntologyVersionMinorAndDomainOntologyVersionPatchAndAgentsDeployingGraphId(
                domainOntologyName,
                domainOntologyVersion.getMajor(),
                domainOntologyVersion.getMinor(),
                domainOntologyVersion.getPatch(),
                agentId);
        return graphRecord;
    }
}



