package org.ubiquia.common.library.belief.state.libraries.interfaces.service.mapper;

import org.ubiquia.common.model.acl.dto.AbstractAclModel;
import org.ubiquia.common.model.acl.entity.AbstractAclModelEntity;

/**
 * An interface defining how DTO mappers should map entities to DTO's.
 *
 * @param <F> The entity class we're mapping from.
 * @param <T> The DTO class we're mapping to.
 */
public interface InterfaceEgressDtoMapper<
    F extends AbstractAclModelEntity,
    T extends AbstractAclModel> {

    /**
     * Get the model type of the entity we're mapping.
     *
     * @return The model type.
     */
    String getModelType();

    /**
     * Generate a new-but-empty DTO.
     *
     * @return The DTO we're generating.
     */
    T getNewDto();
}