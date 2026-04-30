import logging
import subprocess

from util_simulation_service.service.builder.network_builder import NetworkBuilder
from util_simulation_service.model.agent import Agent
from util_simulation_service.model.network import Network

logger = logging.getLogger(__name__)


class DockerNetworkBuilder(NetworkBuilder):
    """Connects agent containers into a Docker bridge network.

    Creates the named Docker network if it does not already exist, then
    attaches each agent's container (identified by agent.name) to it.
    Already-connected containers are silently skipped.
    """

    def build(self, network: Network, agents: list[Agent]) -> None:
        logger.info("Configuring Docker network '%s' for %d agent(s).", network.name, len(agents))
        self._ensure_network(network.name)
        for agent in agents:
            self._connect(network.name, agent)

    def _ensure_network(self, network_name: str) -> None:
        result = subprocess.run(
            ["docker", "network", "ls", "--filter", f"name=^{network_name}$", "--format", "{{.Name}}"],
            check=True,
            capture_output=True,
            text=True,
        )
        if network_name in result.stdout.splitlines():
            logger.info("Docker network '%s' already exists.", network_name)
        else:
            logger.info("Creating Docker network '%s'.", network_name)
            subprocess.run(["docker", "network", "create", network_name], check=True)

    def _disconnect_default_bridge(self, agent: Agent) -> None:
        result = subprocess.run(
            ["docker", "network", "disconnect", "bridge", agent.name],
            capture_output=True,
            text=True,
        )
        if result.returncode == 0:
            logger.info("Disconnected '%s' from default bridge.", agent.name)
        elif "is not connected" in result.stderr or "No such container" in result.stderr:
            pass  # already disconnected — nothing to do
        else:
            result.check_returncode()

    def _connect(self, network_name: str, agent: Agent) -> None:
        result = subprocess.run(
            ["docker", "network", "connect", network_name, agent.name],
            capture_output=True,
            text=True,
        )
        if result.returncode == 0:
            logger.info("Connected '%s' to network '%s'.", agent.name, network_name)
        elif "already exists" in result.stderr:
            logger.info("'%s' is already connected to network '%s'.", agent.name, network_name)
        else:
            result.check_returncode()
