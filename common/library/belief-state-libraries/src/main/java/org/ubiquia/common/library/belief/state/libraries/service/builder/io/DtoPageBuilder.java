package org.ubiquia.common.library.belief.state.libraries.service.builder.io;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.ubiquia.common.library.belief.state.libraries.service.finder.EgressMapperFinder;
import org.ubiquia.common.model.acl.dto.AbstractAclModel;
import org.ubiquia.common.model.acl.entity.AbstractAclModelEntity;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;

@Service
public class DtoPageBuilder<T extends AbstractAclModelEntity, D extends AbstractAclModel> {

    @Autowired
    private EgressMapperFinder egressMapperFinder;

    @Autowired
    private ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public GenericPageImplementation<D> buildPageFrom(Page<T> records)
        throws IllegalAccessException {

        GenericPageImplementation<D> convertedPage;

        var converted = new ArrayList<D>();
        for (var record : records.getContent()) {
            var dtoMapper = this.egressMapperFinder.findEgressMapperFor(record);
            var dto = (D) dtoMapper.map(record);
            converted.add(dto);
        }

        convertedPage = new GenericPageImplementation<>(
            converted,
            records.getNumber(),
            records.getSize(),
            records.getTotalElements(),
            this.objectMapper.valueToTree(records.getPageable()),
            records.isLast(),
            records.getTotalPages(),
            this.objectMapper.valueToTree(records.getSort()),
            records.isFirst(),
            records.getNumberOfElements(),
            records.getSort().isEmpty());

        return convertedPage;
    }
}
