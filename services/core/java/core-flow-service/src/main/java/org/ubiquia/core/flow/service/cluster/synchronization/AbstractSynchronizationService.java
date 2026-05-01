package org.ubiquia.core.flow.service.cluster.synchronization;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.library.implementation.service.mapper.GenericDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.AbstractModel;
import org.ubiquia.common.model.ubiquia.entity.AbstractModelEntity;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;
import org.ubiquia.common.model.ubiquia.entity.SyncEntity;
import org.ubiquia.core.flow.repository.SyncRepository;

public abstract class AbstractSynchronizationService<
    E extends AbstractModelEntity,
    D extends AbstractModel> {

    private static final Logger logger =
        LoggerFactory.getLogger(AbstractSynchronizationService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SyncRepository syncRepository;

    protected abstract AbstractEntityRepository<E> getRepository();

    protected abstract GenericDtoMapper<E, D> getMapper();

    protected abstract String getEndpointPath();

    public void sync(final List<String> peerUrls, final AgentEntity agentEntity) {

        var endpointPath = this.getEndpointPath();

        List<E> entities;
        try {
            entities = this.getRepository().findEntitiesNeedingSync();
        } catch (Exception e) {
            logger.error("Error querying entities for {}: {}", endpointPath, e.getMessage());
            return;
        }

        if (entities.isEmpty()) {
            logger.debug("No entities needing sync for {}.", endpointPath);
            return;
        }

        List<D> dtos;
        try {
            dtos = this.getMapper().map(entities);
        } catch (JsonProcessingException e) {
            logger.error("Error mapping entities for {}: {}", endpointPath, e.getMessage());
            return;
        }

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var allPeersSucceeded = true;
        for (var peerUrl : peerUrls) {
            var fullUrl = peerUrl + endpointPath;
            var peerSucceeded = true;
            for (var dto : dtos) {
                try {
                    var request = new HttpEntity<>(dto, headers);
                    this.restTemplate.postForEntity(fullUrl, request, Void.class);
                } catch (Exception e) {
                    logger.warn("Failed to sync to {}: {}", fullUrl, e.getMessage());
                    peerSucceeded = false;
                }
            }
            if (peerSucceeded) {
                logger.info("Synced {} record(s) to {}.", dtos.size(), fullUrl);
            }
            allPeersSucceeded = allPeersSucceeded && peerSucceeded;
        }

        if (allPeersSucceeded) {
            for (var entity : entities) {
                var syncEntity = new SyncEntity();
                syncEntity.setModel(entity);
                syncEntity.setAgent(agentEntity);
                this.syncRepository.save(syncEntity);
            }
        }
    }
}
