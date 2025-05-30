package org.ubiquia.core.flow.service.registrar;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.models.dto.AgentCommunicationLanguageDto;
import org.ubiquia.common.models.entity.AgentCommunicationLanguage;
import org.ubiquia.core.flow.repository.AgentCommunicationLanguageRepository;
import org.ubiquia.core.flow.service.visitor.validator.JsonSchemaValidator;

@Service
public class AgentCommunicationLanguageRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(
        AgentCommunicationLanguageRegistrar.class);
    @Autowired
    private AgentCommunicationLanguageRepository agentCommunicationLanguageRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JsonSchemaValidator jsonSchemaValidator;

    /**
     * Attempt to register the provided domain ontology with Ubiquia.
     *
     * @param agentCommunicationLanguage The domain ontology to try registering.
     * @return A newly-registered domain ontology.
     * @throws JsonProcessingException Exceptions from parsing JSON schemas.
     */
    @Transactional
    public AgentCommunicationLanguage tryRegister(
        final AgentCommunicationLanguageDto agentCommunicationLanguage)
        throws JsonProcessingException {
        var schema = this.objectMapper.writeValueAsString(agentCommunicationLanguage.getJsonSchema());
        if (!this.jsonSchemaValidator.isValidJsonSchema(schema)) {
            throw new IllegalArgumentException("ERROR: Not a valid JSON Schema!");
        }

        var domainExistsRecord = this
            .agentCommunicationLanguageRepository
            .findByDomainAndVersionMajorAndVersionMinorAndVersionPatch(
                agentCommunicationLanguage.getDomain(),
                agentCommunicationLanguage.getVersion().getMajor(),
                agentCommunicationLanguage.getVersion().getMinor(),
                agentCommunicationLanguage.getVersion().getPatch());

        if (domainExistsRecord.isPresent()) {
            throw new IllegalArgumentException("ERROR: A domain with name '"
                + agentCommunicationLanguage.getDomain()
                + "' and version '"
                + agentCommunicationLanguage.getVersion()
                + "' already exists!");
        }

        agentCommunicationLanguage.setJsonSchema(null);
        var entity = this.objectMapper.convertValue(
            agentCommunicationLanguage,
            AgentCommunicationLanguage.class);
        entity.setJsonSchema(schema);

        if (Objects.isNull(entity.getTags())) {
            entity.setTags(new HashSet<>());
        }

        entity = this.agentCommunicationLanguageRepository.save(entity);
        logger.info("...registered ACL with domain {} and version {} to database...",
            entity.getDomain(),
            entity.getVersion());
        return entity;
    }
}
