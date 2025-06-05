package org.ubiquia.core.communication.service.manager.flow;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.common.model.ubiquia.dto.AdapterDto;
import org.ubiquia.common.model.ubiquia.dto.GraphDto;
import org.ubiquia.core.communication.service.io.flow.DeployedGraphPoller;

@Service
public class AdapterProxyManager {

    private static final Logger logger = LoggerFactory.getLogger(AdapterProxyManager.class);

    @Autowired
    private RestTemplate restTemplate;

    public void tryProcessNewlyDeployedGraph(final GraphDto graph) {

    }

    public void tryProcessNewlyTornDownGraph(final GraphDto graph) {

    }

}
