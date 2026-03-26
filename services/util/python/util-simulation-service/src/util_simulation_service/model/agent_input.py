from pydantic import BaseModel

from util_simulation_service.model.agent_mode import AgentMode


class AgentInput(BaseModel):
    name: str
    mode: AgentMode
