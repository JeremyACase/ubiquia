package org.ubiquia.common.library.api.repository;


import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.ubiquia.common.model.ubiquia.entity.AgentEntity;

/**
 * An interface for Ubiquia Agent entities.
 */
public interface AgentRepository
    extends PagingAndSortingRepository<AgentEntity, String>,
    CrudRepository<AgentEntity, String> {

    /**
     * Query for a specific agent to see if it is deploying the specific graph and version.
     *
     * @param graphName The graph to search for.
     * @param major     The major semantic version.
     * @param minor     The minor semantic version.
     * @param patch     The patch semantic version.
     * @param id        The ID of the Ubiquia agent.
     * @return An optional record with the results.
     */
    Optional<AgentEntity> findByDeployedGraphsNameAndDeployedGraphsDomainOntologyVersionMajorAndDeployedGraphsDomainOntologyVersionMinorAndDeployedGraphsDomainOntologyVersionPatchAndId(
        final String graphName,
        final Integer major,
        final Integer minor,
        final Integer patch,
        final String id);

    /**
     * Customized query method to fetch what Graphs are deployed by a Ubiquia agent provided the
     * agent ID.
     *
     * @param id       The ID of the Ubiquia agent.
     * @param pageable Pageable stuff.
     * @return A page representing the results.
     */
    @Query("SELECT DISTINCT g.id FROM AgentEntity a JOIN a.deployedGraphs g WHERE a.id = :id")
    Page<String> findDeployedGraphIdsById(@Param("id") String id, Pageable pageable);

}
