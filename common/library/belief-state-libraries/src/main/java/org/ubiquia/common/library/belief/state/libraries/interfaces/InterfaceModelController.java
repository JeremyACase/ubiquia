package org.ubiquia.common.library.belief.state.libraries.interfaces;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.ubiquia.common.library.belief.state.libraries.model.association.Association;
import org.ubiquia.common.library.belief.state.libraries.repository.EntityRepository;
import org.ubiquia.common.library.belief.state.libraries.service.builder.entity.EntityRelationshipBuilder;
import org.ubiquia.common.library.belief.state.libraries.service.mapper.AbstractIngressDtoMapper;
import org.ubiquia.common.model.acl.dto.AbstractAclModel;
import org.ubiquia.common.model.acl.embeddable.KeyValuePair;
import org.ubiquia.common.model.acl.entity.AbstractAclModelEntity;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.IngressResponse;

public interface InterfaceModelController<
    T extends AbstractAclModelEntity,
    D extends AbstractAclModel> {

    EntityRelationshipBuilder<T> getEntityRelationshipBuilder();

    EntityRepository<T> getEntityRepository();

    AbstractIngressDtoMapper<D, T> getIngressMapper();

    @Transactional
    IngressResponse add(
        @RequestBody D ingress,
        @RequestParam(value = "message-source", defaultValue = "belief-state") String messageSource)
        throws Exception;

    List<IngressResponse> addList(
        @RequestBody List<D> ingresses,
        @RequestParam(value = "message-source", defaultValue = "belief-state") String messageSource)
        throws Exception;

    IngressResponse addTag(
        @PathVariable("id") final String id,
        @RequestBody KeyValuePair tag,
        @RequestParam(value = "message-source", defaultValue = "belief-state") String messageSource);

    IngressResponse associate(
        @RequestBody @Validated Association association,
        @RequestParam(value = "message-source", defaultValue = "belief-state") String messageSource)
        throws Exception;

    IngressResponse removeTag(
        @PathVariable("id") final String id,
        @RequestBody KeyValuePair tag,
        @RequestParam(value = "message-source", defaultValue = "belief-state") String messageSource);

    List<String> getDistinctKeys();

    List<String> getDistinctValuesByKey(@Param("key") String key);

    IngressResponse updateField(
        @PathVariable String id,
        @RequestBody List<KeyValuePair> keyValuePairs,
        @RequestParam(value = "message-source", defaultValue = "belief-state") String messageSource)
        throws IllegalAccessException;

    ResponseEntity<String> deleteModel(@PathVariable("id") final String id);

    ResponseEntity<D> queryModelWithId(@PathVariable("id") final String id)
        throws NoSuchFieldException,
        IllegalAccessException;

    Long queryCountWithParams(final HttpServletRequest httpServletRequest)
        throws NoSuchFieldException;

    Page<Object[]> queryWithMultiselectParams(
        @RequestParam("page") final Integer page,
        @RequestParam("size") final Integer size,
        @RequestParam(value = "sort-descending", required = false, defaultValue = "true") final Boolean sortDescending,
        @RequestParam(value = "sort-by-fields", required = false, defaultValue = "") final List<String> sortByFields,
        @RequestParam(value = "multiselect-fields", defaultValue = "") final List<String> multiselectFields,
        final HttpServletRequest httpServletRequest)
        throws NoSuchFieldException;

    GenericPageImplementation<D> queryWithParams(
        @RequestParam("page") final Integer page,
        @RequestParam("size") final Integer size,
        @RequestParam(value = "sort-descending", required = false, defaultValue = "true") final Boolean sortDescending,
        @RequestParam(value = "sort-by-fields", required = false, defaultValue = "") final List<String> sortByFields,
        final HttpServletRequest httpServletRequest)
        throws NoSuchFieldException,
        IllegalAccessException;
}
