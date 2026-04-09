import logging

from util_simulation_service.model.agent import Agent
from util_simulation_service.model.agent_mode import AgentMode
from util_simulation_service.model.simulation_input import SimulationInput
from util_simulation_service.service.agent_factory import AgentFactory

logger = logging.getLogger(__name__)


class SetupService:
    """Provisions all agents declared in a SimulationInput using the appropriate builder.

    Agents with mode TEST are assumed to be already running — their base_url
    is taken directly from the AgentInput without any provisioning.
    """

    def __init__(self, agent_factory: AgentFactory):
        self._agent_factory = agent_factory

    def run(self, simulation_input: SimulationInput) -> list[Agent]:
        immediate = [a for a in simulation_input.agents if a.join_offset_time is None]
        logger.info("Setting up %d agent(s) (%d deferred).", len(immediate), len(simulation_input.agents) - len(immediate))
        agents = []
        for agent_input in immediate:
            logger.info("Building agent: %s (mode=%s)", agent_input.name, agent_input.mode.value)
            if agent_input.mode == AgentMode.TEST:
                agent = Agent(name=agent_input.name, base_url=agent_input.base_url)
            else:
                builder = self._agent_factory.get_builder(agent_input.mode)
                agent = builder.build(agent_input.name)
            logger.info("Agent ready: %s at %s", agent.name, agent.base_url)
            agents.append(agent)
        return agents
