package org.ubiquia.common.model.ubiquia.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;

@Entity
public class DomainDataContractEntity extends AbstractModelEntity {

    @Column(columnDefinition = "TEXT")
    private String jsonSchema;

    @OneToOne
    @JoinColumn(name = "domain_ontology_contract_join_id", nullable = true)
    private DomainOntologyEntity domainOntology;

    @Override
    public String getModelType() {
        return "DomainDataContract";
    }

    @NotNull
    public String getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(String jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    public DomainOntologyEntity getDomainOntology() {
        return domainOntology;
    }

    public void setDomainOntology(DomainOntologyEntity domainOntology) {
        this.domainOntology = domainOntology;
    }
}
