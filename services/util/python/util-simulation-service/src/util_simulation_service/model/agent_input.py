from pydantic import BaseModel, model_validator

from util_simulation_service.model.agent_mode import AgentMode
from util_simulation_service.model.graph_deployment_input import GraphDeploymentInput
from util_simulation_service.model.time_offset import TimeOffset


class AgentInput(BaseModel):
    name: str
    mode: AgentMode
    base_url: str | None = None
    join_offset_time: TimeOffset | None = None
    graph_deployments: list[GraphDeploymentInput] = []

    @model_validator(mode="after")
    def validate_test_base_url(self) -> "AgentInput":
        if self.mode == AgentMode.TEST and self.base_url is None:
            raise ValueError("base_url is required when mode is 'test'")
        return self
