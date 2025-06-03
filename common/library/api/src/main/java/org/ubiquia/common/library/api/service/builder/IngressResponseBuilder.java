package org.ubiquia.common.library.api.service.builder;


import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.IngressResponse;
import org.ubiquia.common.model.ubiquia.entity.AbstractEntity;

@Service
public class IngressResponseBuilder {

    /**
     * Build an ingress response for a given database entity.
     *
     * @param entity The entity to build an ingress response for.
     * @return A new ingress response.
     */
    public IngressResponse buildIngressResponseFrom(final AbstractEntity entity) {
        var response = new IngressResponse();
        response.setId(entity.getId());
        response.setPayloadModelType(entity.getModelType());
        return response;
    }
}
