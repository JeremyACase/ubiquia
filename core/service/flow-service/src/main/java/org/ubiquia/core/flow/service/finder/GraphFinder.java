package org.ubiquia.core.flow.service.finder;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;
import org.ubiquia.common.library.config.UbiquiaAgentConfig;
import org.ubiquia.core.flow.repository.GraphRepository;

@Service
@Transactional
public class GraphFinder {
    private static final Logger logger = LoggerFactory.getLogger(GraphFinder.class);

    @Autowired
    private GraphRepository graphRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UbiquiaAgentConfig ubiquiaAgentConfig;

    public Optional<GraphEntity> findGraphFor(
        final String graphName,
        final SemanticVersion version) {
        var record = this
            .graphRepository
            .findByGraphNameAndVersionMajorAndVersionMinorAndVersionPatch(
                graphName,
                version.getMajor(),
                version.getMinor(),
                version.getPatch());

        return record;
    }
}



