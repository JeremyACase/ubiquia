package org.ubiquia.core.flow.component;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.library.implementation.service.mapper.FlowMessageDtoMapper;
import org.ubiquia.core.flow.repository.FlowMessageRepository;
import org.ubiquia.core.flow.service.manager.NodeManager;

@Component
public class FlowEgressRelay {

    private static final Logger logger = LoggerFactory.getLogger(FlowEgressRelay.class);
    private static final long POLL_FREQUENCY_MS = 500L;
    private static final int INBOX_PAGE_SIZE = 10;
    private static final String RECEIVE_ENDPOINT =
        "/ubiquia/core/flow-service/flow-message/receive";

    @Autowired
    private FlowMessageDtoMapper flowMessageDtoMapper;

    @Autowired
    private FlowMessageRepository flowMessageRepository;

    @Autowired
    private NodeManager nodeManager;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final Set<String> peerBaseUrls = ConcurrentHashMap.newKeySet();
    private ScheduledFuture<?> pollTask;

    @PostConstruct
    public void start() {
        var executor = new ScheduledThreadPoolExecutor(1);
        this.pollTask = executor.scheduleAtFixedRate(
            this::tryPollAndForward,
            POLL_FREQUENCY_MS,
            POLL_FREQUENCY_MS,
            TimeUnit.MILLISECONDS);
        logger.info("FlowEgressRelay started.");
    }

    public void updatePeers(final Set<String> peerBaseUrls) {
        this.peerBaseUrls.clear();
        this.peerBaseUrls.addAll(peerBaseUrls);
        logger.debug("FlowEgressRelay peer set updated to {} peer(s).", peerBaseUrls.size());
    }

    @PreDestroy
    public void teardown() {
        logger.info("Tearing down FlowEgressRelay.");
        if (Objects.nonNull(this.pollTask)) {
            this.pollTask.cancel(false);
        }
    }

    void tryPollAndForward() {
        if (this.peerBaseUrls.isEmpty()) {
            return;
        }
        this.transactionTemplate.execute(status -> {
            this.forwardOrphanedMessages(Set.copyOf(this.peerBaseUrls));
            return null;
        });
    }

    private void forwardOrphanedMessages(final Set<String> peers) {
        var localNodeIds = this.nodeManager.getLocalNodeIds();
        var sort = Sort.by("createdAt").ascending();
        var pageRequest = PageRequest.of(0, INBOX_PAGE_SIZE).withSort(sort);
        var allMessages = this.flowMessageRepository.findAll(pageRequest);
        var page = localNodeIds.isEmpty()
            ? allMessages
            : this.flowMessageRepository.findAllByTargetNodeIdNotIn(localNodeIds, pageRequest);

        if (allMessages.hasContent() && !page.hasContent()) {
            logger.info("Relay: {} message(s) in DB but all covered by local node IDs {}.",
                allMessages.getTotalElements(), localNodeIds);
        }

        if (!page.hasContent()) {
            return;
        }

        logger.info("Forwarding {} orphaned message(s) to peers. Local node IDs: {}",
            page.getContent().size(), localNodeIds);

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (var message : page.getContent()) {
            try {
                var dto = this.flowMessageDtoMapper.map(message);
                var openSet = new HashSet<>(peers);

                while (!openSet.isEmpty()) {
                    var peerUrl = openSet.iterator().next();
                    openSet.remove(peerUrl);

                    try {
                        var url = peerUrl + RECEIVE_ENDPOINT;
                        var request = new HttpEntity<>(dto, headers);
                        this.restTemplate.postForEntity(url, request, Void.class);
                        this.flowMessageRepository.deleteById(message.getId());
                        logger.info("Forwarded message {} to {}.", message.getId(), url);
                        break;
                    } catch (Exception e) {
                        logger.warn("Failed to forward message {} to {}: {}",
                            message.getId(), peerUrl, e.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to map message {}: {}", message.getId(), e.getMessage());
            }
        }
    }
}
