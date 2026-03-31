package org.ubiquia.core.flow.service.cluster;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import org.ubiquia.common.library.implementation.service.mapper.ComponentDtoMapper;
import org.ubiquia.common.library.implementation.service.mapper.DomainDataContractDtoMapper;
import org.ubiquia.common.library.implementation.service.mapper.DomainOntologyDtoMapper;
import org.ubiquia.common.library.implementation.service.mapper.FlowDtoMapper;
import org.ubiquia.common.library.implementation.service.mapper.GenericDtoMapper;
import org.ubiquia.common.library.implementation.service.mapper.GraphDtoMapper;
import org.ubiquia.common.library.implementation.service.mapper.NodeDtoMapper;
import org.ubiquia.common.library.implementation.service.mapper.ObjectMetadataDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.AbstractModel;
import org.ubiquia.common.model.ubiquia.entity.AbstractModelEntity;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;
import org.ubiquia.common.model.ubiquia.entity.SyncEntity;
import org.ubiquia.core.flow.repository.ComponentRepository;
import org.ubiquia.core.flow.repository.DomainDataContractRepository;
import org.ubiquia.core.flow.repository.DomainOntologyRepository;
import org.ubiquia.core.flow.repository.FlowRepository;
import org.ubiquia.core.flow.repository.GraphRepository;
import org.ubiquia.core.flow.repository.NodeRepository;
import org.ubiquia.core.flow.repository.ObjectMetadataRepository;
import org.ubiquia.core.flow.repository.SyncRepository;

/**
 * A scheduled service that synchronizes AbstractModelEntity records with peer flow-service
 * instances discovered via the JGroups cluster. An entity is eligible for synchronization when
 * its {@code updatedAt} timestamp is newer than the {@code createdAt} of its most recent
 * {@link SyncEntity}, or when it has never been synced at all.
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
    private ComponentRepository componentRepository;

    @Autowired
    private DomainDataContractRepository domainDataContractRepository;

    @Autowired
    private DomainOntologyRepository domainOntologyRepository;

    @Autowired
    private FlowRepository flowRepository;

    @Autowired
    private GraphRepository graphRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ObjectMetadataRepository objectMetadataRepository;

    @Autowired
    private ComponentDtoMapper componentDtoMapper;

    @Autowired
    private DomainDataContractDtoMapper domainDataContractDtoMapper;

    @Autowired
    private DomainOntologyDtoMapper domainOntologyDtoMapper;

    @Autowired
    private FlowDtoMapper flowDtoMapper;

    @Autowired
    private GraphDtoMapper graphDtoMapper;

    @Autowired
    private NodeDtoMapper nodeDtoMapper;

    @Autowired
    private ObjectMetadataDtoMapper objectMetadataDtoMapper;

    @Value("${server.port}")
    private int serverPort;

    /**
     * Runs on a fixed delay and synchronizes all eligible entity types with every peer
     * agent currently visible in the JGroups cluster view.
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

        this.trySync(componentRepository, componentDtoMapper,
            "/ubiquia/core/flow-service/component/sync", peerUrls, agentEntity);
        this.trySync(domainDataContractRepository, domainDataContractDtoMapper,
            "/ubiquia/core/flow-service/domain-data-contract/sync", peerUrls, agentEntity);
        this.trySync(domainOntologyRepository, domainOntologyDtoMapper,
            "/ubiquia/core/flow-service/domain-ontology/sync", peerUrls, agentEntity);
        this.trySync(flowRepository, flowDtoMapper,
            "/ubiquia/core/flow-service/flow/sync", peerUrls, agentEntity);
        this.trySync(graphRepository, graphDtoMapper,
            "/ubiquia/core/flow-service/graph/sync", peerUrls, agentEntity);
        this.trySync(nodeRepository, nodeDtoMapper,
            "/ubiquia/core/flow-service/node/sync", peerUrls, agentEntity);
        this.trySync(objectMetadataRepository, objectMetadataDtoMapper,
            "/ubiquia/core/flow-service/object-metadata/sync", peerUrls, agentEntity);

        logger.info("...model synchronization complete.");
    }

    /**
     * Finds all entities of type {@code E} that are stale relative to their most recent sync
     * record, maps them to DTOs, posts them to every peer, and records a {@link SyncEntity}
     * for each successfully queued entity.
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
        var request = new HttpEntity<>(dtos, headers);

        for (var peerUrl : peerUrls) {
            var fullUrl = peerUrl + endpointPath;
            try {
                this.restTemplate.postForEntity(fullUrl, request, Void.class);
                logger.info("Synced {} record(s) to {}.", entities.size(), fullUrl);
            } catch (Exception e) {
                logger.warn("Failed to sync to {}: {}", fullUrl, e.getMessage());
            }
        }

        for (var entity : entities) {
            var syncEntity = new SyncEntity();
            syncEntity.setModel(entity);
            syncEntity.setAgent(agentEntity);
            this.syncRepository.save(syncEntity);
        }
    }

    /**
     * Derives HTTP base URLs for all cluster members except the local node, using the IP
     * address advertised by JGroups and the configured {@code server.port}.
     */
    private List<String> resolvePeerUrls() {
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
            if (member instanceof IpAddress ipAddress) {
                var host = ipAddress.getIpAddress().getHostAddress();
                urls.add("http://" + host + ":" + serverPort);
            }
        }
        return urls;
    }
}
