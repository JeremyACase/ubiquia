package org.ubiquia.common.model.ubiquia.dto;


import jakarta.validation.constraints.NotNull;

public class DomainDataContract extends AbstractModel {

    private Object schema;

    private DomainOntology domainOntology;

    @Override
    public String getModelType() {
        return "DomainDataContract";
    }

    @NotNull
    public Object getSchema() {
        return schema;
    }

    public void setSchema(Object schema) {
        this.schema = schema;
    }

    public DomainOntology getDomainOntology() {
        return domainOntology;
    }

    public void setDomainOntology(DomainOntology domainOntology) {
        this.domainOntology = domainOntology;
    }
}
