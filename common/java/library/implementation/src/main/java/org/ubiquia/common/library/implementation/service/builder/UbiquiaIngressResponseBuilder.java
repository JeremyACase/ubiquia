package org.ubiquia.common.library.implementation.service.builder;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.IngressResponse;
import org.ubiquia.common.model.ubiquia.dto.AbstractModel;

/** Service for building {@link IngressResponse} objects from Ubiquia DTO models. */
@Service
public class UbiquiaIngressResponseBuilder {

    private static final Logger logger =
        LoggerFactory.getLogger(UbiquiaIngressResponseBuilder.class);

    /**
     * Build an ingress response from a DTO model.
     *
     * @param model The DTO model to extract ID and model type from.
     * @return A populated {@link IngressResponse}.
     */
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
