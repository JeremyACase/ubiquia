from pydantic import BaseModel

from util_simulation_service.model.domain_ontology_bootstrap_input import DomainOntologyBootstrapInput


class BootstrapInput(BaseModel):
    domain_ontologies: list[DomainOntologyBootstrapInput] = []
