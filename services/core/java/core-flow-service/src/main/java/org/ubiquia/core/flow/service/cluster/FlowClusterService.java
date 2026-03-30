package org.ubiquia.core.flow.service.cluster;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
