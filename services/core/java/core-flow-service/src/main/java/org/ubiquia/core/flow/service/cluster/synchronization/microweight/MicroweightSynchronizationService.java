package org.ubiquia.core.flow.service.cluster.synchronization.microweight;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jgroups.Event;
import org.jgroups.PhysicalAddress;
import org.jgroups.stack.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Resolves HTTP peer URLs for microweight agents — either from the static
 * {@code ubiquia.cluster.sync.peer-base-urls} config (preferred when set by the simulation
 * service) or from the live JGroups cluster view.
 */
@ConditionalOnProperty(
    value = "ubiquia.cluster.sync.microweight.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Service
public class MicroweightSynchronizationService {

    private static final Logger logger =
        LoggerFactory.getLogger(MicroweightSynchronizationService.class);

    @Autowired
    private MicroweightClusterService microweightClusterService;

    @Value("${server.port}")
    private int serverPort;

    @Value("${ubiquia.cluster.sync.peer-base-urls:}")
    private String peerBaseUrlsConfig;

    @Value("${ubiquia.cluster.sync.local-base-url:}")
    private String localBaseUrl;

    /**
     * Returns the base URLs of peer microweight agents.
     *
     * <p>When {@code ubiquia.cluster.sync.peer-base-urls} is set, those URLs are used directly.
     * Otherwise, peers are derived from the live JGroups cluster view.
     */
    public List<String> resolvePeerUrls() {
        if (!this.peerBaseUrlsConfig.isBlank()) {
            return Arrays.stream(this.peerBaseUrlsConfig.split(","))
                .map(String::trim)
                .filter(url -> !url.isBlank() && !url.equals(this.localBaseUrl))
                .toList();
        }
        return this.resolveFromJGroupsView();
    }

    private List<String> resolveFromJGroupsView() {
        var channel = this.microweightClusterService.getChannel();
        if (Objects.isNull(channel) || Objects.isNull(channel.getView())) {
            logger.debug("JGroups channel or view not available; no microweight peers resolved.");
            return List.of();
        }

        var urls = new ArrayList<String>();
        var localAddress = channel.getAddress();
        for (var member : channel.getView().getMembers()) {
            if (member.equals(localAddress)) {
                continue;
            }
            var physAddr = (PhysicalAddress) channel.down(
                new Event(Event.GET_PHYSICAL_ADDRESS, member));
            if (physAddr instanceof IpAddress ipAddress) {
                urls.add("http://" + ipAddress.getIpAddress().getHostAddress() + ":" + serverPort);
            }
        }
        return urls;
    }
}
