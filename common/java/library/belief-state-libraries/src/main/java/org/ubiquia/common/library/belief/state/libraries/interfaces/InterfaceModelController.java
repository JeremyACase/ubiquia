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

/**
 * An interface defining methods for belief state controllers.
 *
 * @param <T> The entity type of the controller.
 * @param <D> The DTO type of the controller.
 */
public interface InterfaceModelController<
    T extends AbstractAclModelEntity,
    D extends AbstractAclModel> {

    /**
     * Get the entity relationship builder for the implementing controller.
     *
     * @return The builder.
     */
    EntityRelationshipBuilder<T> getEntityRelationshipBuilder();

    /**
     * Get the repository interface for the entity class.
     *
     * @return The repository.
     */
    EntityRepository<T> getEntityRepository();

    /**
     * Get the mapper that can read DTO's into entities for this class.
     *
     * @return The mapper.
     */
    AbstractIngressDtoMapper<D, T> getIngressMapper();

    /**
     * A RESTful method allowing the service to ingest new ACL models and persist them.
     *
     * @param ingress The ingress model.
     * @return A response with metadata about the new record.
     * @throws Throwable Exceptions from black magic.
     */
    @Transactional
    IngressResponse add(@RequestBody D ingress) throws Throwable;

    /**
     * Accept a list of models to ingress and persist into the back-end database.
     *
     * @param ingresses The list of models to ingress.
     * @return A list of metadata representing the persisted models.
     * @throws Throwable Exceptions from black magic.
     */
    List<IngressResponse> addList(@RequestBody List<D> ingresses) throws Throwable;

    /**
     * Add a tag to an existing entity in the database.
     *
     * @param id  The ID of the entity to add a tag to.
     * @param tag The tag to append to the entity's existing tags.
     * @return Ingress metadata.
     */
    IngressResponse addTag(@PathVariable("id") final String id, @RequestBody KeyValuePair tag);

    /**
     * Associate two models together using this endpoint.
     *
     * @param association The model housing the data to associate together.
     * @return A response with the ingress data.
     * @throws Throwable Exceptions from black magic.
     */
    IngressResponse associate(@RequestBody @Validated Association association)
        throws Throwable;

    /**
     * Remove a tag from the entity provided the entity's ID.
     *
     * @param id  The ID of the entity.
     * @param tag The tag to remove.
     * @return Ingress metadata confirming a response.
     */
    IngressResponse removeTag(@PathVariable("id") final String id, @RequestBody KeyValuePair tag);

    /**
     * Return the entire list of unique tag keys from this class of models.
     *
     * @return The list of unique keys.
     */
    List<String> getDistinctKeys();

    /**
     * Get the unique values for a given tag key for this model.
     *
     * @param key The key to produce unique values for.
     * @return The list of unique values.
     */
    List<String> getDistinctValuesByKey(@Param("key") String key);

    /**
     * Update the values for specific fields for an entity provided the entity ID.
     *
     * @param id            The ID of the entity.
     * @param keyValuePairs The list of values to update provided the field names as keys.
     * @return Ingress response confirmation.
     * @throws IllegalArgumentException Exceptions from bad requests.
     * @throws IllegalAccessException   Exceptions from reflection.
     */
    IngressResponse updateField(
        @PathVariable String id,
        @RequestBody List<KeyValuePair> keyValuePairs)
        throws IllegalAccessException;

    /**
     * Delete a model provided the ID of the entity to delete.
     *
     * @param id The id of the model to delete.
     * @return A response entity with confirmation.
     */
    ResponseEntity<String> deleteModel(@PathVariable("id") final String id);

    /**
     * Query for an individual models.
     *
     * @param id The ID of the model to query.
     * @return The record if applicable, otherwise a 204.
     * @throws NoSuchFieldException   Exception from missing fields.
     * @throws IllegalAccessException Exception from illegally accessing fields.
     */
    ResponseEntity<D> queryModelWithId(@PathVariable("id") final String id)
        throws Exception;

    /**
     * Query for the number of records applicable for a set of predicates.
     *
     * @param httpServletRequest The request to translate into a set of predicates.
     * @return The number of applicable records.
     * @throws NoSuchFieldException Exception from passing in invalid predicates.
     */
    Long queryCountWithParams(final HttpServletRequest httpServletRequest)
        throws NoSuchFieldException;

    /**
     * A query allowing for no-frills "multi-select" for performance-intensive needs.
     *
     * @param page               The page number to query for.
     * @param size               The size of the page to query for.
     * @param sortDescending     Whether or not to sort descending.
     * @param sortByFields       The fields to sort by.
     * @param multiselectFields  The fields to return on response.
     * @param httpServletRequest The servlet request with any potential predicates.
     * @return The page response with our minimal data.
     * @throws NoSuchFieldException Exceptions from bad client requests.
     */
    Page<Object[]> queryWithMultiselectParams(
        @RequestParam("page") final Integer page,
        @RequestParam("size") final Integer size,
        @RequestParam(value = "sort-descending", required = false, defaultValue = "true") final Boolean sortDescending,
        @RequestParam(value = "sort-by-fields", required = false, defaultValue = "") final List<String> sortByFields,
        @RequestParam(value = "multiselect-fields", defaultValue = "") final List<String> multiselectFields,
        final HttpServletRequest httpServletRequest)
        throws NoSuchFieldException;

    /**
     * Query for page of data provided predicates.
     *
     * @param page               The page number to query for.
     * @param size               The size of the page to query for.
     * @param sortDescending     Whether or not to sort descending.
     * @param sortByFields       The fields to sort by.
     * @param httpServletRequest The servlet request with any potential predicates.
     * @return The page response with our minimal data.
     * @throws NoSuchFieldException Exceptions from bad client requests.
     */
    GenericPageImplementation<D> queryWithParams(
        @RequestParam("page") final Integer page,
        @RequestParam("size") final Integer size,
        @RequestParam(value = "sort-descending", required = false, defaultValue = "true") final Boolean sortDescending,
        @RequestParam(value = "sort-by-fields", required = false, defaultValue = "") final List<String> sortByFields,
        final HttpServletRequest httpServletRequest)
        throws Exception;
}
