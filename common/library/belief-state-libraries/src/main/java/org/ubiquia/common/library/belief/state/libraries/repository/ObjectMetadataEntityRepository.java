package org.ubiquia.common.library.belief.state.libraries.repository;

import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.model.ubiquia.entity.ObjectMetadataEntity;

/**
 * An interface for "object metadata" - or the relational metadata for binaries and other
 * files stored in any Ubiquia object storages.
 */
public interface ObjectMetadataEntityRepository
    extends AbstractEntityRepository<ObjectMetadataEntity> {

}
