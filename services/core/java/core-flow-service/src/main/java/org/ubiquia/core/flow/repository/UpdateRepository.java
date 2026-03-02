package org.ubiquia.core.flow.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.ubiquia.common.model.ubiquia.entity.UpdateEntity;

public interface UpdateRepository extends JpaRepository<UpdateEntity, String>,
    CrudRepository<UpdateEntity, String> {

}
