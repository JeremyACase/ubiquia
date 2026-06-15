package org.ubiquia.core.belief.state.generator.service.generator.postprocess;

import java.io.IOException;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;

/** Coordinates post-generation cleanup and support-file injection steps. */
@Service
public class BeliefStateGenerationPostProcessor {

    @Autowired
    private BeliefStateGenerationCleanupProcessor beliefStateGenerationCleanupProcessor;

    @Autowired
    private BeliefStateGenerationSupportProcessor beliefStateGenerationSupportProcessor;

    /**
     * Runs cleanup and support processors against the generated output for {@code domainOntology}.
     */
    public void postProcess(final DomainOntology domainOntology) throws IOException {

        this.beliefStateGenerationCleanupProcessor.removeBlacklistedFiles(Paths.get("generated"));
        this.beliefStateGenerationSupportProcessor.postProcess(domainOntology);

    }
}
