package org.ubiquia.common.model.ubiquia.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;

@Entity
public class DomainDataContractEntity extends AbstractModelEntity {

    @Column(columnDefinition = "TEXT")
    private String schema;

    @OneToOne
    @JoinColumn(name = "domain_ontology_contract_join_id", nullable = true)
    private DomainOntologyEntity domainOntology;

    @Override
    public String getModelType() {
        return "DomainDataContract";
    }

    @NotNull
    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public DomainOntologyEntity getDomainOntology() {
        return domainOntology;
    }

    public void setDomainOntology(DomainOntologyEntity domainOntology) {
        this.domainOntology = domainOntology;
    }
}
