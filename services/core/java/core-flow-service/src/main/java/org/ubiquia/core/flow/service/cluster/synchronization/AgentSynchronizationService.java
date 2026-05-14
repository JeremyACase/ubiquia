package org.ubiquia.core.flow.service.cluster.synchronization;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.common.library.implementation.service.mapper.AgentDtoMapper;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;

/**
 * Pushes this agent's own record to peer agents so they can track it for heartbeat monitoring.
 *
 * <p>Only runs when this agent has a {@code baseUrl} configured — microweight agents set this
 * to their Docker-network address so Kubernetes peers can ping them back. Agents without a
 * baseUrl (e.g. Kubernetes agents that are pure receivers) skip the push silently.
 */
@Service
public class AgentSynchronizationService {

    private static final Logger logger = LoggerFactory.getLogger(AgentSynchronizationService.class);
    private static final String ENDPOINT_PATH = "/ubiquia/core/flow-service/agent/register";

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private AgentDtoMapper agentDtoMapper;

    @Autowired
    private RestTemplate restTemplate;

    public void sync(final List<String> peerUrls) {
        if (peerUrls.isEmpty()) {
            return;
        }

        var agentEntity = this.agentRepository.findById(this.agentConfig.getId()).orElse(null);
        if (agentEntity == null
            || agentEntity.getBaseUrl() == null
            || agentEntity.getBaseUrl().isBlank()) {
            logger.debug("Skipping agent registration push: no baseUrl configured on this agent.");
            return;
        }

        var dto = this.mapAgent(agentEntity);
        if (dto == null) {
            return;
        }

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new HttpEntity<>(dto, headers);

        for (var peerUrl : peerUrls) {
            try {
                this.restTemplate.postForEntity(peerUrl + ENDPOINT_PATH, request, Void.class);
                logger.debug("Pushed agent registration to {}.", peerUrl);
            } catch (Exception e) {
                logger.warn("Failed to push agent registration to {}: {}", peerUrl, e.getMessage());
            }
        }
    }

    private Object mapAgent(final AgentEntity entity) {
        try {
            return this.agentDtoMapper.map(entity);
        } catch (Exception e) {
            logger.error("Failed to map local agent for registration push: {}", e.getMessage());
            return null;
        }
    }
}
