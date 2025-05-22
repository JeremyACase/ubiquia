package org.ubiquia.core.flow.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.models.dto.AbstractEntityDto;
import org.ubiquia.common.models.entity.AbstractEntity;

/**
 * A class dedicated to mapping from entities from the database to Data Transfer
 * Objects (DTO's).
 */
@Service
public abstract class GenericDtoMapper<F extends AbstractEntity, T extends AbstractEntityDto>
    implements InterfaceEntityToDtoMapper<F, T> {

    @Autowired
    protected ObjectMapper objectMapper;

    @Override
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
    protected void setAbstractEntityFields(final F from, T to) {

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
