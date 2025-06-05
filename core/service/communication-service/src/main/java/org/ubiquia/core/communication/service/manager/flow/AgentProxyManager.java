package org.ubiquia.core.communication.service.manager.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.ubiquia.common.model.ubiquia.dto.GraphDto;

@Service
public class AgentProxyManager {
    private static final Logger logger = LoggerFactory.getLogger(AdapterProxyManager.class);

    @Autowired
    private WebClient webClient;

    public void tryProcessNewlyDeployedGraph(final GraphDto graph) {

    }

    public void tryProcessNewlyTornDownGraph(final GraphDto graph) {

    }
}
