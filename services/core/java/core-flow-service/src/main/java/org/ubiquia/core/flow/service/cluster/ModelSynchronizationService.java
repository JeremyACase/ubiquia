package org.ubiquia.core.flow.service.cluster;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jgroups.Event;
import org.jgroups.PhysicalAddress;
import org.jgroups.stack.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.common.library.implementation.service.mapper.DomainOntologyDtoMapper;
import org.ubiquia.common.library.implementation.service.mapper.GenericDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.AbstractModel;
import org.ubiquia.common.model.ubiquia.entity.AbstractModelEntity;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;
import org.ubiquia.common.model.ubiquia.entity.SyncEntity;
import org.ubiquia.core.flow.repository.DomainOntologyRepository;
import org.ubiquia.core.flow.repository.SyncRepository;

/**
 * A scheduled service that synchronizes {@link org.ubiquia.common.model.ubiquia.entity.DomainOntologyEntity}
 * records with peer flow-service instances. Registering a domain ontology on the peer cascades
 * creation of all child entities (data contracts, graphs, nodes, components), so only the
 * top-level ontology needs to be pushed.
 *
 * <p>Enabled only when {@code ubiquia.cluster.flow-service.sync.enabled=true}.
 */
@ConditionalOnProperty(
    value = "ubiquia.cluster.flow-service.sync.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class ModelSynchronizationService {

    private static final Logger logger =
        LoggerFactory.getLogger(ModelSynchronizationService.class);

    private static final String DOMAIN_ONTOLOGY_REGISTER_PATH =
        "/ubiquia/core/flow-service/domain-ontology/register/post";

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private FlowClusterService flowClusterService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SyncRepository syncRepository;

    @Autowired
    private DomainOntologyRepository domainOntologyRepository;

    @Autowired
    private DomainOntologyDtoMapper domainOntologyDtoMapper;

    @Value("${server.port}")
    private int serverPort;

    @Value("${ubiquia.cluster.sync.peer-base-urls:}")
    private String peerBaseUrlsConfig;

    @Value("${ubiquia.cluster.sync.local-base-url:}")
    private String localBaseUrl;

    /**
     * Runs on a fixed delay and synchronizes domain ontologies with every peer agent
     * currently visible in the cluster.
     */
    @Scheduled(
        fixedDelayString = "${ubiquia.cluster.flow-service.sync.frequency-milliseconds:30000}")
    @Transactional
    public void synchronize() {
        var peerUrls = this.resolvePeerUrls();
        if (peerUrls.isEmpty()) {
            logger.debug("No peers in cluster view; skipping synchronization.");
            return;
        }

        logger.info("Starting model synchronization with {} peer(s)...", peerUrls.size());

        var agentEntity = this.agentRepository.findById(this.agentConfig.getId()).orElse(null);
        if (Objects.isNull(agentEntity)) {
            logger.error("Cannot sync: agent record not found for id {}.", this.agentConfig.getId());
            return;
        }

        this.trySync(domainOntologyRepository, domainOntologyDtoMapper,
            DOMAIN_ONTOLOGY_REGISTER_PATH, peerUrls, agentEntity);

        logger.info("...model synchronization complete.");
    }

    /**
     * Finds all entities of type {@code E} that are stale relative to their most recent sync
     * record, maps them to DTOs, posts each one individually to the given endpoint on every peer,
     * and records a {@link SyncEntity} for each entity once at least one peer has accepted it.
     */
    private <E extends AbstractModelEntity, D extends AbstractModel> void trySync(
        AbstractEntityRepository<E> repository,
        GenericDtoMapper<E, D> mapper,
        String endpointPath,
        List<String> peerUrls,
        AgentEntity agentEntity) {

        List<E> entities;
        try {
            entities = repository.findEntitiesNeedingSync();
        } catch (Exception e) {
            logger.error("Error querying entities for path {}: {}", endpointPath, e.getMessage());
            return;
        }

        if (entities.isEmpty()) {
            logger.debug("No entities needing sync for {}.", endpointPath);
            return;
        }

        List<D> dtos;
        try {
            dtos = mapper.map(entities);
        } catch (JsonProcessingException e) {
            logger.error("Error mapping entities for {}: {}", endpointPath, e.getMessage());
            return;
        }

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var atLeastOnePeerSucceeded = false;
        for (var peerUrl : peerUrls) {
            var fullUrl = peerUrl + endpointPath;
            var allSucceeded = true;
            for (var dto : dtos) {
                try {
                    var request = new HttpEntity<>(dto, headers);
                    this.restTemplate.postForEntity(fullUrl, request, Void.class);
                } catch (Exception e) {
                    logger.warn("Failed to sync to {}: {}", fullUrl, e.getMessage());
                    allSucceeded = false;
                }
            }
            if (allSucceeded) {
                logger.info("Synced {} record(s) to {}.", dtos.size(), fullUrl);
                atLeastOnePeerSucceeded = true;
            }
        }

        if (atLeastOnePeerSucceeded) {
            for (var entity : entities) {
                var syncEntity = new SyncEntity();
                syncEntity.setModel(entity);
                syncEntity.setAgent(agentEntity);
                this.syncRepository.save(syncEntity);
            }
        }
    }

    /**
     * Derives HTTP base URLs for peer flow-service instances.
     *
     * <p>When {@code ubiquia.cluster.sync.peer-base-urls} is set (comma-separated), those URLs
     * are used directly and {@code ubiquia.cluster.sync.local-base-url} is excluded from the
     * list. This mode is preferred in environments (e.g. Kubernetes) where JGroups TCPPING
     * discovery is unreliable due to all nodes starting simultaneously.
     *
     * <p>When the static list is absent, peers are derived from the live JGroups cluster view
     * using the IP address advertised by each member and the configured {@code server.port}.
     */
    private List<String> resolvePeerUrls() {
        if (!this.peerBaseUrlsConfig.isBlank()) {
            return Arrays.stream(this.peerBaseUrlsConfig.split(","))
                .map(String::trim)
                .filter(url -> !url.isBlank() && !url.equals(this.localBaseUrl))
                .collect(Collectors.toList());
        }

        var urls = new ArrayList<String>();
        var channel = this.flowClusterService.getChannel();

        if (Objects.isNull(channel) || Objects.isNull(channel.getView())) {
            return urls;
        }

        var localAddress = channel.getAddress();
        for (var member : channel.getView().getMembers()) {
            if (member.equals(localAddress)) {
                continue;
            }
            var physAddr = (PhysicalAddress) channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, member));
            if (physAddr instanceof IpAddress ipAddress) {
                var host = ipAddress.getIpAddress().getHostAddress();
                urls.add("http://" + host + ":" + serverPort);
            }
        }
        return urls;
    }
}
