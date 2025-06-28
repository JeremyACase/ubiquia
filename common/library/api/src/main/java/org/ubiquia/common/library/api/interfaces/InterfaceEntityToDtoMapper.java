package org.ubiquia.common.library.api.interfaces;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.ubiquia.common.model.ubiquia.dto.AbstractModel;

/**
 * An interface that can be used to map from database entities to their Data Transfer Object
 * representations.
 *
 * @param <F> The entity class we're mapping from.
 * @param <T> The DTO class we're mapping to.
 */
public interface InterfaceEntityToDtoMapper<
    F extends org.ubiquia.common.model.ubiquia.entity.AbstractModelEntity,
    T extends AbstractModel> {

    /**
     * Map a Ubiquia entity to a DTO.
     *
     * @param from The entity to map from.
     * @return A DTO mapped from the entity.
     * @throws JsonProcessingException Exception from mapping payload strings to objects.
     */
    T map(F from) throws JsonProcessingException;

    /**
     * Map from a list of database entities to a list of DTO's.
     *
     * @param froms The entities to map from.
     * @return The mapped DTO's.
     * @throws JsonProcessingException Exception from mapping payload strings to objects.
     */
    List<T> map(List<F> froms) throws JsonProcessingException;
}
