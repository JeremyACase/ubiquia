import logging
import pathlib
import subprocess
import uuid

from util_simulation_service.builder.agent_builder import AgentBuilder
from util_simulation_service.model.agent import Agent
from util_simulation_service.model.agent_mode import AgentMode

logger = logging.getLogger(__name__)

_IMAGE_NAME = "ubiquia/core-flow-service"
_CONTAINER_PORT = "8080"


class MicroweightAgentBuilder(AgentBuilder):
    """Provisions a Ubiquia agent as a bare-metal Docker container running core-flow-service.

    Mirrors the behaviour of bare-metal-microweight-install.sh:
      - Builds the core-flow-service image from source.
      - Starts one container per agent with a randomly assigned host port.
      - Mounts the shared application config, ontologies, and graphs from the repo.
    """

    def __init__(self, repo_root: pathlib.Path):
        self._repo_root = repo_root
        self._service_dir = repo_root / "services" / "core" / "java" / "core-flow-service"
        self._config = repo_root / "deploy" / "compose" / "config" / "application.yaml"
        self._ontologies = repo_root / "deploy" / "helm" / "bootstrap" / "ontologies"
        self._graphs = repo_root / "deploy" / "helm" / "bootstrap" / "graphs"

    def build(self, agent_name: str) -> Agent:
        self._build_image()
        self._remove_existing_container(agent_name)
        agent_id = str(uuid.uuid4())
        logger.info("Starting microweight container '%s' (agent_id=%s).", agent_name, agent_id)
        self._run_container(agent_name, agent_id=agent_id)
        host_port = self._get_host_port(agent_name)
        logger.info("Container '%s' listening on http://localhost:%s.", agent_name, host_port)
        return Agent(name=agent_name, base_url=f"http://localhost:{host_port}", mode=AgentMode.MICROWEIGHT)

    def _build_image(self) -> None:
        logger.info("Building Docker image: %s.", _IMAGE_NAME)
        subprocess.run(
            ["docker", "build", "--build-arg", "OPENJDK_VERSION=21", "-t", _IMAGE_NAME, str(self._service_dir)],
            check=True,
        )

    def _remove_existing_container(self, name: str) -> None:
        result = subprocess.run(
            ["docker", "ps", "-a", "--filter", f"name=^{name}$", "--format", "{{.Names}}"],
            check=True,
            capture_output=True,
            text=True,
        )
        if name in result.stdout.splitlines():
            logger.info("Removing existing container '%s'.", name)
            subprocess.run(["docker", "rm", "-f", name], check=True)

    def _run_container(self, name: str, agent_id: str) -> None:
        cmd = [
            "docker", "run", "-d",
            "--name", name,
            "-p", _CONTAINER_PORT,
            "-e", f"UBIQUIA_AGENT_ID={agent_id}",
            "-e", "UBIQUIA_MODE=TEST",
            "-v", f"{self._config}:/app/etc/application.yaml:ro",
            "-v", f"{self._ontologies}:/app/etc/domain-ontologies:ro",
        ]
        if self._graphs.exists():
            cmd += ["-v", f"{self._graphs}:/app/etc/graphs:ro"]
        cmd.append(_IMAGE_NAME)
        subprocess.run(cmd, check=True)

    def _get_host_port(self, name: str) -> str:
        """Return the host port Docker assigned to the container's port 8080."""
        result = subprocess.run(
            ["docker", "port", name, _CONTAINER_PORT],
            check=True,
            capture_output=True,
            text=True,
        )
        # Output: "0.0.0.0:32768\n" or ":::32768\n" — port is always the last colon-delimited token.
        return result.stdout.splitlines()[0].rsplit(":", 1)[-1]
