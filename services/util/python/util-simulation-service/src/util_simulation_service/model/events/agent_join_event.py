from typing import Literal

from util_simulation_service.model.agent_input import AgentInput
from util_simulation_service.model.events.event import Event


class AgentJoinEvent(Event):
    type: Literal["agent_join"] = "agent_join"
    agent: AgentInput
