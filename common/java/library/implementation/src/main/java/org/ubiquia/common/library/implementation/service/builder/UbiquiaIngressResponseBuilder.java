package org.ubiquia.common.library.implementation.service.builder;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.IngressResponse;
import org.ubiquia.common.model.ubiquia.dto.AbstractModel;

@Service
public class UbiquiaIngressResponseBuilder {

    private static final Logger logger = LoggerFactory.getLogger(UbiquiaIngressResponseBuilder.class);

    public IngressResponse buildIngressResponseFrom(final AbstractModel model) {
        logger.debug("...building ingress response for model with: \nid: {} \ntype: {}",
            model.getId(),
            model.getModelType());
        var response = new IngressResponse();
        response.setId(model.getId());
        response.setPayloadModelType(model.getModelType());
        return response;
    }
}
