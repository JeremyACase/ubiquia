package org.ubiquia.core.flow.service.registrar;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.DomainOntology;
import org.ubiquia.common.model.ubiquia.entity.DomainDataContractEntity;
import org.ubiquia.common.model.ubiquia.entity.DomainOntologyEntity;
import org.ubiquia.core.flow.repository.DomainDataContractRepository;
import org.ubiquia.core.flow.service.visitor.validator.JsonSchemaValidator;

@Service
public class DomainDataContractRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(DomainDataContractRegistrar.class);

    @Autowired
    private JsonSchemaValidator jsonSchemaValidator;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DomainDataContractRepository domainDataContractRepository;

    public void tryRegisterDomainDataContract(
        DomainOntologyEntity domainOntologyEntity,
        final DomainOntology domainOntology)
        throws JsonProcessingException {

        logger.info("Registering domain data contract for ontology: {}...",
            domainOntology.getName());

        this.tryValidateDomainDataContract(domainOntology);
        this.tryPersistDomainDataContract(domainOntologyEntity, domainOntology);

        logger.info("...registered domain data contract for ontology: {}.",
            domainOntologyEntity.getName());
    }

    private void tryValidateDomainDataContract(final DomainOntology domainOntology)
        throws JsonProcessingException {

        if (Objects.isNull(domainOntology.getDomainDataContract())) {
            throw new IllegalArgumentException("ERROR: A domain data contract cannot be null!");
        }

        var stringifiedSchema = this.objectMapper.writeValueAsString(domainOntology
            .getDomainDataContract()
            .getSchema());

        this.jsonSchemaValidator.isValidJsonSchema(stringifiedSchema);
    }

    @Transactional
    private DomainDataContractEntity tryPersistDomainDataContract(
        DomainOntologyEntity domainOntologyEntity,
        final DomainOntology domainOntology)
        throws JsonProcessingException {

        var domainDataContractEntity = new DomainDataContractEntity();
        domainDataContractEntity.setDomainOntology(domainOntologyEntity);

        var stringifiedSchema = this.objectMapper.writeValueAsString(domainOntology
            .getDomainDataContract()
            .getSchema());

        domainDataContractEntity.setSchema(stringifiedSchema);

        domainDataContractEntity = this
            .domainDataContractRepository
            .save(domainDataContractEntity);

        domainOntologyEntity.setDomainDataContract(domainDataContractEntity);

        return domainDataContractEntity;
    }
}
