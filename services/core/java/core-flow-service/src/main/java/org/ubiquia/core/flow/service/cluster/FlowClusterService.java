package org.ubiquia.core.flow.service.cluster;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ThreadLocalRandom;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Manages a JGroups TCP cluster channel for peer-to-peer communication between
 * microweight core-flow-service instances.
 *
 * <p>Enabled by setting {@code ubiquia.sync.enabled=true}. Uses TCPPING for
 * static host discovery, suitable for Docker-based microweight deployments where
 * peer addresses are known at startup.
 */
@ConditionalOnProperty(
    value = "ubiquia.cluster.flow-service.sync.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class FlowClusterService implements Receiver {

    private static final Logger logger = LoggerFactory.getLogger(FlowClusterService.class);

    @Value("${ubiquia.cluster.seed-hosts}")
    private String seedHosts;

    @Value("${ubiquia.cluster.name}")
    private String clusterName;

    @Value("${ubiquia.cluster.port}")
    private int port;

    @Value("${ubiquia.cluster.rejoin-delay-milliseconds:5000}")
    private long rejoinDelayMs;

    private JChannel channel;

    @PostConstruct
    public void start() throws Exception {
        logger.info("Starting JGroups cluster '{}' on port {} with seed hosts: {}",
            clusterName, port, seedHosts);

        System.setProperty("jgroups.tcpping.initial_hosts", seedHosts);
        System.setProperty("jgroups.bind_port", String.valueOf(port));

        this.channel = new JChannel("jgroups-tcp.xml");
        this.channel.setReceiver(this);
        this.channel.connect(clusterName);

        logger.info("JGroups cluster '{}' joined — local address: {}",
            clusterName, this.channel.getAddress());
    }

    @PreDestroy
    public void stop() {
        if (this.channel != null) {
            logger.info("Closing JGroups cluster channel '{}'.", clusterName);
            this.channel.close();
        }
    }

    /**
     * Runs on a fixed delay. If this node is the sole member of its cluster, it disconnects
     * and reconnects — triggering a fresh TCPPING discovery. Peer agents that have come up in
     * the meantime will be found on the next attempt, preventing a permanent split that neither
     * MERGE3 nor the application can recover from on its own.
     */
    @Scheduled(fixedDelayString = "${ubiquia.cluster.rejoin-delay-milliseconds:5000}",
               initialDelayString = "${ubiquia.cluster.rejoin-delay-milliseconds:5000}")
    public void rejoinIfSolo() throws InterruptedException {
        if (this.channel == null) {
            return;
        }
        var view = this.channel.getView();
        if (view == null || view.getMembers().size() > 1) {
            return;
        }

        // Random jitter in [0, rejoinDelayMs) so that agents starting simultaneously
        // stagger their reconnects — preventing all peers from disconnecting at once
        // and missing each other on every attempt.
        long jitter = ThreadLocalRandom.current().nextLong(this.rejoinDelayMs);
        logger.info("Solo cluster detected; retrying join in {}ms (jitter)...", jitter);
        Thread.sleep(jitter);

        // Re-check after the sleep — a peer may have already joined us.
        view = this.channel.getView();
        if (view != null && view.getMembers().size() > 1) {
            return;
        }

        try {
            this.channel.disconnect();
            this.channel.connect(this.clusterName);
            logger.info("After rejoin — view: {}", this.channel.getView().getMembers());
        } catch (Exception e) {
            logger.warn("Rejoin attempt failed: {}", e.getMessage());
        }
    }

    @Override
    public void viewAccepted(View newView) {
        logger.info("JGroups cluster view updated — members: {}", newView.getMembers());
    }

    @Override
    public void receive(Message msg) {
        logger.debug("JGroups message received from {}: {} bytes",
            msg.getSrc(), msg.getLength());
    }

    public JChannel getChannel() {
        return channel;
    }
}
