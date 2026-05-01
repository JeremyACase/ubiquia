package org.ubiquia.core.flow.service.cluster;

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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.config.AgentConfig;
import org.ubiquia.common.library.api.repository.AgentRepository;
import org.ubiquia.core.flow.service.cluster.synchronization.AbstractSynchronizationService;

/**
 * Scheduled orchestrator that drives all registered {@link AbstractSynchronizationService}
 * implementations on a fixed delay. Enabled only when
 * {@code ubiquia.cluster.flow-service.sync.enabled=true}.
 */
@ConditionalOnProperty(
    value = "ubiquia.cluster.flow-service.sync.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class UbiquiaSynchronizationService {

    private static final Logger logger =
        LoggerFactory.getLogger(UbiquiaSynchronizationService.class);

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private FlowClusterService flowClusterService;

    @Autowired
    private List<AbstractSynchronizationService<?, ?>> synchronizationServices;

    @Value("${server.port}")
    private int serverPort;

    @Value("${ubiquia.cluster.sync.peer-base-urls:}")
    private String peerBaseUrlsConfig;

    @Value("${ubiquia.cluster.sync.local-base-url:}")
    private String localBaseUrl;

    @Scheduled(
        fixedDelayString = "${ubiquia.cluster.flow-service.sync.frequency-milliseconds:30000}")
    @Transactional
    public void synchronize() {
        var peerUrls = this.resolvePeerUrls();
        if (peerUrls.isEmpty()) {
            logger.debug("No peers in cluster view; skipping synchronization.");
            return;
        }

        logger.info("Starting synchronization with {} peer(s)...", peerUrls.size());

        var agentEntity = this.agentRepository.findById(this.agentConfig.getId()).orElse(null);
        if (Objects.isNull(agentEntity)) {
            logger.error("Cannot sync: agent record not found for id {}.", this.agentConfig.getId());
            return;
        }

        for (var service : this.synchronizationServices) {
            service.sync(peerUrls, agentEntity);
        }

        logger.info("...synchronization complete.");
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
            var physAddr = (PhysicalAddress) channel.down(
                new Event(Event.GET_PHYSICAL_ADDRESS, member));
            if (physAddr instanceof IpAddress ipAddress) {
                var host = ipAddress.getIpAddress().getHostAddress();
                urls.add("http://" + host + ":" + serverPort);
            }
        }
        return urls;
    }
}
