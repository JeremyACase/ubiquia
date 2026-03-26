import logging

from util_simulation_service.builder.network_builder import NetworkBuilder
from util_simulation_service.model.agent import Agent
from util_simulation_service.model.network import Network
from util_simulation_service.model.network_topology import NetworkTopology
from util_simulation_service.model.simulation_input import SimulationInput

logger = logging.getLogger(__name__)

_DEFAULT_NETWORK_NAME = "default"


class NetworkService:
    """Connects agents into the networks declared in a SimulationInput.

    Resolves each network's agent names against the provisioned Agent objects
    returned by SetupService, then delegates to NetworkBuilder to establish
    the underlying connectivity. Returns a NetworkTopology describing which
    agents can communicate with which.

    If no networks are declared, all agents are placed into a single default
    network so they can communicate freely.
    """

    def __init__(self, network_builder: NetworkBuilder):
        self._network_builder = network_builder

    def run(self, simulation_input: SimulationInput, agents: list[Agent]) -> NetworkTopology:
        topology = NetworkTopology()
        networks = simulation_input.networks

        if not networks:
            logger.info(
                "No networks defined — placing all %d agent(s) in a single '%s' network.",
                len(agents),
                _DEFAULT_NETWORK_NAME,
            )
            networks = [Network(name=_DEFAULT_NETWORK_NAME, agents=[a.name for a in agents])]

        agent_map = {agent.name: agent for agent in agents}

        for network in networks:
            logger.info("Setting up network '%s' (%d agent(s)).", network.name, len(network.agents))
            network_agents = [agent_map[name] for name in network.agents]
            self._network_builder.build(network, network_agents)

            for name in network.agents:
                for peer in network.agents:
                    if name != peer:
                        topology.add_peer(name, peer)

        return topology
