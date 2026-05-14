import logging

from util_simulation_service.model.agent import Agent
from util_simulation_service.model.agent_input import AgentInput
from util_simulation_service.model.agent_mode import AgentMode
from util_simulation_service.model.simulation_input import SimulationInput
from util_simulation_service.service.factory.agent_factory import AgentFactory

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
                agent = Agent(name=agent_input.name, base_url=agent_input.base_url, mode=AgentMode.TEST)
            elif agent_input.mode == AgentMode.MICROWEIGHT:
                sync_enabled, kubernetes_peer_urls = _microweight_peer_config(agent_input.name, simulation_input)
                builder = self._agent_factory.get_builder(agent_input.mode)
                agent = builder.build(
                    agent_input.name,
                    sync_enabled=sync_enabled,
                    kubernetes_peer_urls=kubernetes_peer_urls,
                )
            else:
                builder = self._agent_factory.get_builder(agent_input.mode)
                agent = builder.build(agent_input.name)
            logger.info("Agent ready: %s at %s", agent.name, agent.base_url)
            agents.append(agent)
        return agents


def _microweight_peer_config(
    agent_name: str, simulation_input: SimulationInput
) -> tuple[bool, list[str]]:
    """Return (sync_enabled, kubernetes_peer_urls) for a microweight agent.

    sync_enabled is True when the agent shares a network with at least one
    other agent (microweight or Kubernetes). kubernetes_peer_urls contains the
    base URLs of any TEST-mode (Kubernetes) peers in the same network, which
    the agent will use for cross-cluster HTTP synchronization pushes.
    """
    agent_map: dict[str, AgentInput] = {a.name: a for a in simulation_input.agents}
    has_microweight_peers = False
    kubernetes_peer_urls: list[str] = []

    for network in simulation_input.networks:
        if agent_name not in network.agents:
            continue
        for peer_name in network.agents:
            if peer_name == agent_name:
                continue
            peer = agent_map.get(peer_name)
            if peer is None:
                continue
            if peer.mode == AgentMode.MICROWEIGHT:
                has_microweight_peers = True
            elif peer.mode == AgentMode.TEST and peer.base_url:
                kubernetes_peer_urls.append(peer.base_url)

    sync_enabled = has_microweight_peers or bool(kubernetes_peer_urls)
    return sync_enabled, kubernetes_peer_urls
