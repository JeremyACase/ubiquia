package org.ubiquia.core.flow.service.cluster.synchronization.microweight;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Manages the microweight agent cluster channel, currently implemented with JGroups TCP.
 *
 * <p>Responsible for maintaining membership and topology awareness among co-located
 * microweight (Docker-based) Ubiquia agents. The underlying clustering technology may
 * change, but this service will always own that responsibility.
 *
 * <p>Enabled by setting {@code ubiquia.cluster.flow-service.sync.enabled=true}.
 */
@ConditionalOnProperty(
    value = "ubiquia.cluster.sync.microweight.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class MicroweightClusterService implements Receiver {

    private static final Logger logger = LoggerFactory.getLogger(MicroweightClusterService.class);

    @Value("${ubiquia.cluster.seed-hosts}")
    private String seedHosts;

    @Value("${ubiquia.cluster.name}")
    private String clusterName;

    @Value("${ubiquia.cluster.port}")
    private int port;

    @Value("${ubiquia.cluster.bind-addr:GLOBAL}")
    private String bindAddr;

    @Value("${ubiquia.cluster.rejoin-delay-milliseconds:5000}")
    private long rejoinDelayMs;

    @Autowired
    private MicroweightNetworkManager microweightNetworkManager;

    private JChannel channel;

    /** Joins the JGroups TCP cluster on startup. */
    @PostConstruct
    public void start() throws Exception {
        logger.info("Starting JGroups cluster '{}' on port {} with seed hosts: {}",
            clusterName, port, seedHosts);

        System.setProperty("jgroups.tcpping.initial_hosts", seedHosts);
        System.setProperty("jgroups.bind_port", String.valueOf(port));
        System.setProperty("jgroups.bind_addr", this.bindAddr);

        this.channel = new JChannel("jgroups-tcp.xml");
        this.channel.setReceiver(this);
        this.channel.connect(clusterName);

        logger.info("JGroups cluster '{}' joined — local address: {}",
            clusterName, this.channel.getAddress());
    }

    /** Closes the JGroups channel on application shutdown. */
    @PreDestroy
    public void stop() {
        if (Objects.nonNull(this.channel)) {
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
        if (Objects.isNull(this.channel)) {
            return;
        }
        var view = this.channel.getView();
        if (Objects.isNull(view) || view.getMembers().size() > 1) {
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
        if (Objects.nonNull(view) && view.getMembers().size() > 1) {
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
        if (newView.getMembers().size() == 1) {
            this.microweightNetworkManager.onSoloView();
        } else {
            this.microweightNetworkManager.onClusterView();
        }
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
