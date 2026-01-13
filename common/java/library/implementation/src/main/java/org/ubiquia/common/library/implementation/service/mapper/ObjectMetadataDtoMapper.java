package org.ubiquia.common.library.implementation.service.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.ObjectMetadata;
import org.ubiquia.common.model.ubiquia.entity.ObjectMetadataEntity;


@Service
public class ObjectMetadataDtoMapper extends GenericDtoMapper<
    ObjectMetadataEntity,
    ObjectMetadata> {

    @Autowired
    private UbiquiaAgentDtoMapper ubiquiaAgentDtoMapper;

    @Override
    public ObjectMetadata map(final ObjectMetadataEntity from) throws JsonProcessingException {

        ObjectMetadata to = null;
        if (Objects.nonNull(from)) {
            to = new ObjectMetadata();
            super.setAbstractEntityFields(from, to);

            to.setBucketName(from.getBucketName());
            to.setContentType(from.getContentType());
            to.setOriginalFilename(from.getOriginalFilename());
            to.setUbiquiaAgent(this.ubiquiaAgentDtoMapper.map(from.getAgent()));
            to.setSize(from.getSize());

        }
        return to;
    }
}
