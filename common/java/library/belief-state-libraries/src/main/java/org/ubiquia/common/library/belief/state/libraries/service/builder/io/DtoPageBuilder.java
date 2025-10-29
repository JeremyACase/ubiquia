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

/**
 * A service dedicated to building paginated responses for Ubiquia belief states.
 *
 * @param <T> The entity class we're building pages for.
 * @param <D> The DTO class we're egressing in the paginated responses.
 */
@Service
public class DtoPageBuilder<T extends AbstractAclModelEntity, D extends AbstractAclModel> {

    @Autowired
    private EgressMapperFinder egressMapperFinder;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Build a paginated response from a page of database of records.
     *
     * @param records The database records to egress as a page of DTOs.
     * @return paginated response of DTOs.
     * @throws IllegalAccessException Exceptions from reflection.
     */
    @SuppressWarnings("unchecked")
    public GenericPageImplementation<D> buildPageFrom(Page<T> records)
        throws Exception {

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
