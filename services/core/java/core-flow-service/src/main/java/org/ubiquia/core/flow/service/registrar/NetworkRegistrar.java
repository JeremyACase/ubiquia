package org.ubiquia.core.flow.service.registrar;

import jakarta.transaction.Transactional;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Network;
import org.ubiquia.common.model.ubiquia.entity.NetworkEntity;
import org.ubiquia.core.flow.repository.NetworkRepository;

@Service
public class NetworkRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(NetworkRegistrar.class);

    @Autowired
    private NetworkRepository networkRepository;

    @Transactional
    public void tryRegister(final Network dto) {
        if (Objects.nonNull(dto.getId()) && this.networkRepository.existsById(dto.getId())) {
            logger.debug("Network {} already exists; skipping.", dto.getId());
            return;
        }
        var entity = new NetworkEntity();
        if (Objects.nonNull(dto.getId())) {
            entity.setId(dto.getId());
        }
        this.networkRepository.save(entity);
        logger.info("Registered Network {}.", entity.getId());
    }
}
