package org.ubiquia.core.flow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.ubiquia.common.model.ubiquia.entity.NetworkEntity;

public interface NetworkRepository extends JpaRepository<NetworkEntity, String>,
    CrudRepository<NetworkEntity, String> {

}
