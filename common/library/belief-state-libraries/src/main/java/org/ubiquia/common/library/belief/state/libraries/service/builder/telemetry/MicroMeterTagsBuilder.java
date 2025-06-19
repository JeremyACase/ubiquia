package org.ubiquia.common.library.belief.state.libraries.service.builder.telemetry;


import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.belief.state.libraries.controller.AbstractAclModelController;
import org.ubiquia.common.model.acl.embeddable.KeyValuePair;

@SuppressWarnings("rawtypes")
@Service
public class MicroMeterTagsBuilder {

    private static final Logger logger = LoggerFactory.getLogger(MicroMeterTagsBuilder.class);

    public List<KeyValuePair> buildControllerTagsFor(final AbstractAclModelController controller) {

        var tags = new ArrayList<KeyValuePair>();

        return tags;
    }
}
