import logging

from util_simulation_service.builder.network_builder import NetworkBuilder
from util_simulation_service.model.agent import Agent
from util_simulation_service.model.simulation_input import SimulationInput

logger = logging.getLogger(__name__)


class NetworkService:
    """Connects agents into the networks declared in a SimulationInput.

    Resolves each network's agent names against the provisioned Agent objects
    returned by SetupService, then delegates to NetworkBuilder to establish
    the underlying connectivity.
    """

    def __init__(self, network_builder: NetworkBuilder):
        self._network_builder = network_builder

    def run(self, simulation_input: SimulationInput, agents: list[Agent]) -> None:
        if not simulation_input.networks:
            logger.info("No networks defined — skipping network setup.")
            return

        agent_map = {agent.name: agent for agent in agents}

        for network in simulation_input.networks:
            logger.info("Setting up network '%s' (%d agent(s)).", network.name, len(network.agents))
            network_agents = [agent_map[name] for name in network.agents]
            self._network_builder.build(network, network_agents)
