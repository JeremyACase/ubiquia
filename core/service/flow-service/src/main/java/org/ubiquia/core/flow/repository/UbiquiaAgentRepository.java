package org.ubiquia.core.flow.repository;


import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.ubiquia.common.model.ubiquia.entity.UbiquiaAgent;

public interface UbiquiaAgentRepository extends PagingAndSortingRepository<UbiquiaAgent, String>,
    CrudRepository<UbiquiaAgent, String> {

    Optional<UbiquiaAgent> findByDeployedGraphsGraphNameAndDeployedGraphsVersionMajorAndDeployedGraphsVersionMinorAndDeployedGraphsVersionPatchAndId(
        final String graphName,
        final Integer major,
        final Integer minor,
        final Integer patch,
        final String id);

    @Query("SELECT g.id FROM UbiquiaAgent a JOIN a.deployedGraphs g WHERE a.id = :agentId")
    Page<String> findDeployedGraphIdsById(
        final String agentId,
        final Pageable pageable);

}
