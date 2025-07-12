package org.ubiquia.core.flow.repository;

import java.util.Optional;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.model.ubiquia.entity.AgentCommunicationLanguageEntity;


public interface AgentCommunicationLanguageRepository
    extends AbstractEntityRepository<AgentCommunicationLanguageEntity> {

    Optional<AgentCommunicationLanguageEntity> findByDomainAndVersionMajorAndVersionMinorAndVersionPatch(
        final String domain,
        final Integer major,
        final Integer minor,
        final Integer patch);

}
