package org.ubiquia.common.library.api.repository;


import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.ubiquia.common.model.ubiquia.entity.UbiquiaAgentEntity;

public interface UbiquiaAgentRepository extends PagingAndSortingRepository<UbiquiaAgentEntity, String>,
    CrudRepository<UbiquiaAgentEntity, String> {

    Optional<UbiquiaAgentEntity> findByDeployedGraphsNameAndDeployedGraphsVersionMajorAndDeployedGraphsVersionMinorAndDeployedGraphsVersionPatchAndId(
        final String graphName,
        final Integer major,
        final Integer minor,
        final Integer patch,
        final String id);

    @Query("SELECT DISTINCT g.id FROM UbiquiaAgentEntity a JOIN a.deployedGraphs g WHERE a.id = :agentId")
    Page<String> findDeployedGraphIdsById(
        final String agentId,
        final Pageable pageable);

}
