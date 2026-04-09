from pydantic import BaseModel, model_validator

from util_simulation_service.model.agent_mode import AgentMode


class AgentInput(BaseModel):
    name: str
    mode: AgentMode
    base_url: str | None = None

    @model_validator(mode="after")
    def validate_test_base_url(self) -> "AgentInput":
        if self.mode == AgentMode.TEST and self.base_url is None:
            raise ValueError("base_url is required when mode is 'test'")
        return self
