package org.ubiquia.core.flow.service.finder;

import jakarta.transaction.Transactional;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.embeddable.SemanticVersion;
import org.ubiquia.common.model.ubiquia.entity.UbiquiaAgentEntity;
import org.ubiquia.common.library.config.UbiquiaAgentConfig;
import org.ubiquia.core.flow.repository.UbiquiaAgentRepository;

@Service
@Transactional
public class UbiquiaAgentFinder {
    private static final Logger logger = LoggerFactory.getLogger(UbiquiaAgentFinder.class);

    @Autowired
    private UbiquiaAgentRepository ubiquiaAgentRepository;

    @Autowired
    private UbiquiaAgentConfig ubiquiaAgentConfig;

    public Optional<UbiquiaAgentEntity> findAgentFor(String graphName, SemanticVersion version) {
        var ubiquiaAgentRecord = this
            .ubiquiaAgentRepository
            .findByDeployedGraphsGraphNameAndDeployedGraphsVersionMajorAndDeployedGraphsVersionMinorAndDeployedGraphsVersionPatchAndId(
                graphName,
                version.getMajor(),
                version.getMinor(),
                version.getPatch(),
                this.ubiquiaAgentConfig.getId());
        return ubiquiaAgentRecord;
    }
}



