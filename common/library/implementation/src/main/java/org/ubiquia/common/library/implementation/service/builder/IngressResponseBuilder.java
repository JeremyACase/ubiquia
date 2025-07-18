package org.ubiquia.common.library.implementation.service.builder;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.acl.entity.AbstractAclModelEntity;
import org.ubiquia.common.model.ubiquia.IngressResponse;
import org.ubiquia.common.model.ubiquia.entity.AbstractModelEntity;

@Service
public class IngressResponseBuilder {

    private static final Logger logger = LoggerFactory.getLogger(IngressResponseBuilder.class);

    public IngressResponse buildIngressResponseFor(final AbstractAclModelEntity entity) {
        logger.debug("...building ingress response for entity with: \nid: {} \ntype: {}",
            entity.getId(),
            entity.getModelType());
        var response = new IngressResponse();
        response.setId(entity.getId());
        response.setPayloadModelType(entity.getModelType());
        return response;
    }

    public IngressResponse buildIngressResponseFrom(final AbstractModelEntity entity) {
        logger.debug("...building ingress response for entity with: \nid: {} \ntype: {}",
            entity.getId(),
            entity.getModelType());
        var response = new IngressResponse();
        response.setId(entity.getId());
        response.setPayloadModelType(entity.getModelType());
        return response;
    }
}
