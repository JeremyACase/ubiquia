import logging

from util_simulation_service.command.event_command import EventCommand
from util_simulation_service.model.agent import Agent
from util_simulation_service.model.events.agent_join_event import AgentJoinEvent
from util_simulation_service.model.events.event import Event
from util_simulation_service.model.agent_mode import AgentMode
from util_simulation_service.service.agent_factory import AgentFactory

logger = logging.getLogger(__name__)


class AgentJoinEventCommand(EventCommand):
    """Provisions a deferred agent at its scheduled join time and adds it to the live agent list.

    For TEST-mode agents the pod is assumed to be already running; the command simply
    registers it with the simulation. For all other modes the agent is built on demand
    via the AgentFactory before being registered.
    """

    def __init__(self, agents: list[Agent], agent_factory: AgentFactory) -> None:
        self._agents = agents
        self._agent_factory = agent_factory

    def execute(self, event: Event) -> None:
        if not isinstance(event, AgentJoinEvent):
            raise TypeError(f"Expected AgentJoinEvent, got {type(event).__name__}")

        agent_input = event.agent
        logger.info("Agent joining simulation: %s (mode=%s)", agent_input.name, agent_input.mode.value)

        if agent_input.mode == AgentMode.TEST:
            agent = Agent(name=agent_input.name, base_url=agent_input.base_url)
        else:
            builder = self._agent_factory.get_builder(agent_input.mode)
            agent = builder.build(agent_input.name)

        self._agents.append(agent)
        logger.info("Agent joined: %s at %s", agent.name, agent.base_url)
