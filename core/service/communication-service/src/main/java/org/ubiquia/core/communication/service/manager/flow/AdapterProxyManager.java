package org.ubiquia.core.communication.service.manager.flow;

import java.util.HashMap;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AdapterDto;

@Service
public class AdapterProxyManager {

    private HashMap<String, AdapterDto> cachedAdapters;

}
