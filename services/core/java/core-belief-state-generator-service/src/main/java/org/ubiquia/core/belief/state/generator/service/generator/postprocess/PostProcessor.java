package org.ubiquia.core.belief.state.generator.service.generator.postprocess;

import java.io.IOException;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.AgentCommunicationLanguage;

@Service
public class PostProcessor {

    @Autowired
    private GenerationCleanupProcessor generationCleanupProcessor;

    @Autowired
    private GenerationSupportProcessor generationSupportProcessor;

    public void postProcess(final AgentCommunicationLanguage acl) throws IOException {

        this.generationCleanupProcessor.removeBlacklistedFiles(Paths.get("generated"));
        this.generationSupportProcessor.postProcess(acl);

    }
}
