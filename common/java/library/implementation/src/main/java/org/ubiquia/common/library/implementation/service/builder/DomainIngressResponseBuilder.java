package org.ubiquia.common.library.implementation.service.builder;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.domain.entity.AbstractDomainModelEntity;
import org.ubiquia.common.model.ubiquia.IngressResponse;

/** Service for building {@link IngressResponse} objects from domain model entities. */
@Service
public class DomainIngressResponseBuilder {

    private static final Logger logger =
        LoggerFactory.getLogger(DomainIngressResponseBuilder.class);

    /**
     * Build an ingress response from a domain entity.
     *
     * @param entity The domain entity to extract ID and model type from.
     * @return A populated {@link IngressResponse}.
     */
    public IngressResponse buildIngressResponseFrom(final AbstractDomainModelEntity entity) {
        logger.debug("...building ingress response for entity with: \nid: {} \ntype: {}",
            entity.getUbiquiaId(),
            entity.getModelType());
        var response = new IngressResponse();
        response.setId(entity.getUbiquiaId());
        response.setPayloadModelType(entity.getModelType());
        return response;
    }
}
