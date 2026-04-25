from pydantic import BaseModel

from util_simulation_service.model.agent_mode import AgentMode


class Agent(BaseModel):
    name: str
    base_url: str
    mode: AgentMode = AgentMode.TEST
