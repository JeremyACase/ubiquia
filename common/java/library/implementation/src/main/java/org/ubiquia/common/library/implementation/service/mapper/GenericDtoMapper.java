package org.ubiquia.common.library.implementation.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.model.ubiquia.dto.AbstractModel;
import org.ubiquia.common.model.ubiquia.entity.AbstractModelEntity;

/**
 * A class dedicated to mapping from Amigos Event entities from the database to Data Transfer
 * Objects (DTO's.)
 */
@Service
public abstract class GenericDtoMapper<
    F extends AbstractModelEntity,
    T extends AbstractModel>
    implements InterfaceEntityToDtoMapper<F, T> {

    @Autowired
    protected ObjectMapper objectMapper;

    public List<T> map(final List<F> froms) throws JsonProcessingException {
        var tos = new ArrayList<T>();
        for (var from : froms) {
            var to = this.map(from);
            tos.add(to);
        }
        return tos;
    }

    /**
     * A helper method that can set the fields that are always present in an AEntity model.
     *
     * @param from The entity model to map from.
     * @param to   The DTO to map to.
     */
    public void setAbstractEntityFields(F from, T to) {

        if (Objects.nonNull(from) && Objects.nonNull(to)) {
            to.setCreatedAt(from.getCreatedAt());
            to.setId(from.getId());
            to.setUpdatedAt(from.getUpdatedAt());

            if (Objects.nonNull(from.getTags())) {
                to.setTags(from.getTags().stream().toList());
            } else {
                to.setTags(new ArrayList<>());
            }
        }
    }
}