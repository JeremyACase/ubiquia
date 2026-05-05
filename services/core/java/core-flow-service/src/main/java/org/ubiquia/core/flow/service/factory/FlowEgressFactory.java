package org.ubiquia.core.flow.service.factory;

import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.component.FlowEgressRelay;

@Service
public class FlowEgressFactory {

    private static final Logger logger = LoggerFactory.getLogger(FlowEgressFactory.class);

    @Autowired
    private ApplicationContext applicationContext;

    private FlowEgressRelay relay;

    public synchronized void tryBuildEgressFor(final Set<String> peerBaseUrls) {
        if (peerBaseUrls.isEmpty()) {
            this.teardown();
            return;
        }

        if (Objects.isNull(this.relay)) {
            logger.info("Building FlowEgressRelay for {} peer(s)...", peerBaseUrls.size());
            this.relay = this.applicationContext.getBean(FlowEgressRelay.class);
            this.relay.initialize(peerBaseUrls);
            logger.info("...FlowEgressRelay running.");
        } else {
            this.relay.updatePeers(peerBaseUrls);
        }
    }

    public synchronized void teardown() {
        if (Objects.nonNull(this.relay)) {
            this.relay.teardown();
            this.relay = null;
        }
    }
}
