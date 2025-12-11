package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.NotNull;

public class DomainDataContract extends AbstractModel {

    private Object jsonSchema;

    private DomainOntology domainOntology;

    @Override
    public String getModelType() {
        return "DomainDataContract";
    }

    @NotNull
    public Object getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(Object jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    public DomainOntology getDomainOntology() {
        return domainOntology;
    }

    public void setDomainOntology(DomainOntology domainOntology) {
        this.domainOntology = domainOntology;
    }
}
