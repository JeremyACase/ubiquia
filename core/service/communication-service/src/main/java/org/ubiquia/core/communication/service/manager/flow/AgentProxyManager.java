package org.ubiquia.core.communication.service.manager.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.GraphDto;

@Service
public class AgentProxyManager {

    private static final Logger logger = LoggerFactory.getLogger(AdapterProxyManager.class);

    public void tryProcessNewlyDeployedGraph(final GraphDto graph) {

    }

    public void tryProcessNewlyTornDownGraph(final GraphDto graph) {

    }

}
