package org.ubiquia.core.communication.service.manager.flow;

import java.util.HashMap;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AgentDto;

@Service
public class AgentProxyManager {

    private HashMap<String, AgentDto> cachedAgents;

}
