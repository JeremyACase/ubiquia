import logging

from util_simulation_service.model.agent import Agent
from util_simulation_service.model.agent_mode import AgentMode
from util_simulation_service.model.simulation_input import SimulationInput
from util_simulation_service.service.agent_factory import AgentFactory

logger = logging.getLogger(__name__)


class SetupService:
    """Provisions all agents declared in a SimulationInput using the appropriate builder."""

    def __init__(self, agent_factory: AgentFactory):
        self._agent_factory = agent_factory

    def run(self, simulation_input: SimulationInput, mode: AgentMode) -> list[Agent]:
        logger.info("Setting up %d agent(s) in %s mode.", len(simulation_input.agents), mode.value)
        builder = self._agent_factory.get_builder(mode)
        agents = []
        for agent_name in simulation_input.agents:
            logger.info("Building agent: %s", agent_name)
            agent = builder.build(agent_name)
            logger.info("Agent ready: %s at %s", agent.name, agent.base_url)
            agents.append(agent)
        return agents
