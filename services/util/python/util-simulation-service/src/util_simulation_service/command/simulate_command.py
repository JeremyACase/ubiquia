import pathlib
import subprocess

import click

from util_simulation_service.builder.docker_network_builder import DockerNetworkBuilder
from util_simulation_service.builder.kind_agent_builder import KindAgentBuilder
from util_simulation_service.builder.microweight_agent_builder import MicroweightAgentBuilder
from util_simulation_service.command.simulation_event_command import SimulationEventCommand
from util_simulation_service.model.agent_mode import AgentMode
from util_simulation_service.service.agent_factory import AgentFactory
from util_simulation_service.service.analysis_service import AnalysisService
from util_simulation_service.service.event_manager import EventManager
from util_simulation_service.service.network_service import NetworkService
from util_simulation_service.service.setup_service import SetupService
from util_simulation_service.service.simulation_service import SimulationService


def _repo_root() -> pathlib.Path:
    result = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        check=True,
        capture_output=True,
        text=True,
    )
    return pathlib.Path(result.stdout.strip())


@click.command("simulate")
@click.option(
    "--mode",
    type=click.Choice([m.value for m in AgentMode], case_sensitive=False),
    required=True,
    help="The mode of the Ubiquia agent deployment to target.",
)
@click.option(
    "--input-file",
    type=click.Path(exists=True, dir_okay=False, path_type=pathlib.Path),
    required=True,
    help="JSON file describing the simulation (name, agents, events, networks, speed).",
)
def simulate(mode: str, input_file: pathlib.Path):
    """Replay a set of JSON events against a live Ubiquia deployment."""

    simulation_input = SimulationService.load(input_file)
    repo_root = _repo_root()

    agents = SetupService(
        agent_factory=AgentFactory(
            microweight_builder=MicroweightAgentBuilder(repo_root=repo_root),
            kind_builder=KindAgentBuilder(repo_root=repo_root),
        )
    ).run(simulation_input=simulation_input, mode=AgentMode(mode))

    NetworkService(
        network_builder=DockerNetworkBuilder()
    ).run(simulation_input=simulation_input, agents=agents)

    SimulationService(
        simulation_input=simulation_input,
        event_manager=EventManager(
            commands={"simulation": SimulationEventCommand()}
        ),
    ).run()

    AnalysisService().run()
