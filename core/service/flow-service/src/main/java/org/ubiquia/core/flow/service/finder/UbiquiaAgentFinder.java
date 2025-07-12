package org.ubiquia.core.flow.service.finder;

import jakarta.transaction.Transactional;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.entity.UbiquiaAgentEntity;
import org.ubiquia.common.library.api.config.UbiquiaAgentConfig;
import org.ubiquia.common.library.api.repository.UbiquiaAgentRepository;

@Service
@Transactional
public class UbiquiaAgentFinder {

    private static final Logger logger = LoggerFactory.getLogger(UbiquiaAgentFinder.class);

    @Autowired
    private UbiquiaAgentRepository ubiquiaAgentRepository;

    @Autowired
    private UbiquiaAgentConfig ubiquiaAgentConfig;

    public Optional<UbiquiaAgentEntity> findAgentFor(final GraphDeployment graphDeployment) {
        var ubiquiaAgentRecord = this
            .ubiquiaAgentRepository
            .findByDeployedGraphsNameAndDeployedGraphsVersionMajorAndDeployedGraphsVersionMinorAndDeployedGraphsVersionPatchAndId(
                graphDeployment.getName(),
                graphDeployment.getVersion().getMajor(),
                graphDeployment.getVersion().getMinor(),
                graphDeployment.getVersion().getPatch(),
                this.ubiquiaAgentConfig.getId());
        return ubiquiaAgentRecord;
    }
}



