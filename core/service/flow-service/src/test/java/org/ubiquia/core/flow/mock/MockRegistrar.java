package org.ubiquia.core.flow.mock;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ubiquia.core.flow.controller.AgentCommunicationLanguageController;
import org.ubiquia.core.flow.controller.GraphController;
import org.ubiquia.core.flow.model.dto.AgentCommunicationLanguageDto;
import org.ubiquia.core.flow.model.embeddable.SemanticVersion;
import org.ubiquia.core.flow.model.entity.AgentCommunicationLanguage;
import org.ubiquia.core.flow.repository.AgentCommunicationLanguageRepository;


@Service
public class MockRegistrar {

    @Value("${ubiquia.test.acl.schema.filepath}")
    private String schemaFilepath;

    @Autowired
    private AgentCommunicationLanguageController aclController;

    @Autowired
    private GraphController graphController;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AgentCommunicationLanguageRepository aclRepository;

    /**
     * Register a domain ontology for testing.
     *
     * @return The domain ontology registered.
     * @throws IOException Exception from reading from disk.
     */
    @Transactional
    public AgentCommunicationLanguage tryRegisterAcl() throws IOException {
        AgentCommunicationLanguage acl = null;

        var existingRecord = this
            .aclRepository
            .findByDomainAndVersionMajorAndVersionMinorAndVersionPatch(
                "pets",
                1,
                2,
                3);

        if (existingRecord.isEmpty()) {
            var schemaPath = Paths.get(this.schemaFilepath);
            var jsonSchema = this.objectMapper.readValue(
                schemaPath.toFile(),
                Object.class);

            var registration = new AgentCommunicationLanguageDto();
            registration.setJsonSchema(jsonSchema);
            registration.setDomain("pets");
            registration.setVersion(new SemanticVersion());
            registration.getVersion().setMajor(1);
            registration.getVersion().setMinor(2);
            registration.getVersion().setPatch(3);

            var ingressResponse = this.aclController.register(registration);
            acl = this.aclRepository.findById(ingressResponse.getId()).get();
        } else {
            acl = existingRecord.get();
        }

        return acl;
    }
}
