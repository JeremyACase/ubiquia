package org.ubiquia.common.library.belief.state.libraries.service.builder.telemetry;


import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.belief.state.libraries.controller.AbstractAclModelController;
import org.ubiquia.common.model.acl.embeddable.KeyValuePair;

/**
 * A builder service to help ensure our Ubiquia multi-agent system provides observability in a
 * way that's decoupled from the application logic.
 */
@SuppressWarnings("rawtypes")
@Service
public class MicroMeterTagsBuilder {

    private static final Logger logger = LoggerFactory.getLogger(MicroMeterTagsBuilder.class);

    /**
     * Build tags for a given controller.
     *
     * @param controller The controller to build tags for.
     * @return A list of key-value-pair tags.
     */
    public List<KeyValuePair> buildControllerTagsFor(
        final AbstractAclModelController controller) {

        var tags = new ArrayList<KeyValuePair>();

        return tags;
    }
}
