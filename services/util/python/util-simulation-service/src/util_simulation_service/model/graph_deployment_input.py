from pydantic import BaseModel

from util_simulation_service.model.semantic_version import SemanticVersion


class GraphDeploymentInput(BaseModel):
    graph_name: str
    domain_ontology_name: str
    domain_version: SemanticVersion
    flag: str | None = None
