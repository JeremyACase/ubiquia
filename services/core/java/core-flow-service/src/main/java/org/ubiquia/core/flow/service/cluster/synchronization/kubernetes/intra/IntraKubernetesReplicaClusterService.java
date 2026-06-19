package org.ubiquia.core.flow.service.cluster.synchronization.kubernetes.intra;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Objects;
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
 * Manages a JGroups cluster among replicas of this K8s Deployment using KUBE_PING
 * for automatic pod discovery. Only active when {@code ubiquia.kubernetes.enabled=true}.
 *
 * <p>The sole purpose of this cluster is leader election: exactly one replica is the
 * JGroups coordinator at any time. Callers check {@link #isLeader()} before running
 * work that must execute on only one replica (scheduled tasks, outbox processing, etc.).
 * When the leader pod fails, JGroups automatically promotes the next member.
 *
 * <p>This channel is entirely separate from the microweight JGroups channel managed by
 * {@link
 * org.ubiquia.core.flow.service.cluster.synchronization.microweight.MicroweightClusterService};
 * the two never share a cluster.
 */
@ConditionalOnProperty(value = "ubiquia.kubernetes.enabled", havingValue = "true")
@Service
public class IntraKubernetesReplicaClusterService implements Receiver {

    private static final Logger logger =
        LoggerFactory.getLogger(IntraKubernetesReplicaClusterService.class);

    private static final String REPLICA_CLUSTER_NAME = "ubiquia-replica-cluster";

    @Value("${ubiquia.kubernetes.namespace:ubiquia}")
    private String namespace;

    @Value("${ubiquia.cluster.port:7800}")
    private int port;

    private JChannel channel;

    /** Joins the JGroups KUBE_PING cluster for replica leader election. */
    @PostConstruct
    public void start() throws Exception {
        logger.info("Starting K8s replica JGroups cluster '{}' (namespace={}, port={})...",
            REPLICA_CLUSTER_NAME, this.namespace, this.port);

        System.setProperty("jgroups.bind_port", String.valueOf(this.port));
        System.setProperty("jgroups.kube.namespace", this.namespace);
        System.setProperty("jgroups.kube.labels", "component=ubiquia-core-flow-service");

        this.channel = new JChannel("jgroups-kube.xml");
        this.channel.setReceiver(this);
        this.channel.connect(REPLICA_CLUSTER_NAME);

        logger.info("K8s replica cluster joined — local address: {}, leader: {}",
            this.channel.getAddress(), this.isLeader());
    }

    /** Closes the JGroups channel on application shutdown. */
    @PreDestroy
    public void stop() {
        if (Objects.nonNull(this.channel)) {
            logger.info("Closing K8s replica cluster channel.");
            this.channel.close();
        }
    }

    /**
     * Returns true if this replica is the current JGroups coordinator (leader).
     * The coordinator is always the first member of the view — JGroups guarantees
     * this is consistent across all members of the cluster.
     */
    public boolean isLeader() {
        if (Objects.isNull(this.channel)) {
            return false;
        }
        View view = this.channel.getView();
        if (Objects.isNull(view) || view.getMembers().isEmpty()) {
            return false;
        }
        return view.getMembers().get(0).equals(this.channel.getAddress());
    }

    @Override
    public void viewAccepted(View newView) {
        logger.info("K8s replica cluster view updated — members: {}, leader: {}",
            newView.getMembers(), this.isLeader());
    }

    @Override
    public void receive(Message msg) {
        // No application messages sent on this channel — it is used solely for membership.
    }
}
