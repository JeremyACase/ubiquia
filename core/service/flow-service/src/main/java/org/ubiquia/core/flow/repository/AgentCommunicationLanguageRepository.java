package org.ubiquia.core.flow.repository;

import java.util.Optional;
import org.ubiquia.common.model.ubiquia.entity.AgentCommunicationLanguage;


public interface AgentCommunicationLanguageRepository
    extends AbstractEntityRepository<AgentCommunicationLanguage> {

    Optional<AgentCommunicationLanguage> findByDomainAndVersionMajorAndVersionMinorAndVersionPatch(
        final String domain,
        final Integer major,
        final Integer minor,
        final Integer patch);

}
