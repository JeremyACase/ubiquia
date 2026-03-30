package org.ubiquia.core.flow.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.ubiquia.common.model.ubiquia.entity.SyncEntity;

public interface SyncRepository extends JpaRepository<SyncEntity, String>,
    CrudRepository<SyncEntity, String> {

}
