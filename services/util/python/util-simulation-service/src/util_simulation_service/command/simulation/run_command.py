import pathlib
import subprocess

import click

from util_simulation_service.builder.docker_network_builder import DockerNetworkBuilder
from util_simulation_service.builder.kind_agent_builder import KindAgentBuilder
from util_simulation_service.builder.microweight_agent_builder import MicroweightAgentBuilder
from util_simulation_service.command.agent_join_event_command import AgentJoinEventCommand
from util_simulation_service.command.partition_event_command import PartitionEventCommand
from util_simulation_service.command.simulation_event_command import SimulationEventCommand
from util_simulation_service.model.events.agent_join_event import AgentJoinEvent
from util_simulation_service.model.agent_mode import AgentMode
from util_simulation_service.model.network_topology import NetworkTopology
from util_simulation_service.service.agent_factory import AgentFactory
from util_simulation_service.service.analysis_service import AnalysisService
from util_simulation_service.service.clock_broadcast_service import ClockBroadcastService
from util_simulation_service.service.domain_ontology_bootstrap_service import DomainOntologyBootstrapService
from util_simulation_service.service.event_dump_service import EventDumpService
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


@click.command("run")
@click.option(
    "--input-file",
    type=click.Path(exists=True, dir_okay=False, path_type=pathlib.Path),
    required=True,
    help="YAML file describing the simulation (name, agents, events, networks, speed).",
)
@click.option(
    "--output-path",
    type=click.Path(file_okay=False, writable=True, path_type=pathlib.Path),
    default=pathlib.Path("."),
    show_default=True,
    help="Directory in which the event dump JSON file is written.",
)
@click.option(
    "--output-file-name",
    type=str,
    default=None,
    help="Filename for the event dump (default: {simulation-name}-event-dump.json).",
)
def run(input_file: pathlib.Path, output_path: pathlib.Path, output_file_name: str | None):
    """Run a simulation against a live Ubiquia deployment."""

    simulation_input = SimulationService.load(input_file)

    # Only resolve the repo root (requires git) when non-test agents are present.
    needs_repo_root = any(a.mode != AgentMode.TEST for a in simulation_input.agents)
    repo_root = _repo_root() if needs_repo_root else pathlib.Path(".")

    agent_factory = AgentFactory(
        microweight_builder=MicroweightAgentBuilder(repo_root=repo_root),
        kind_builder=KindAgentBuilder(repo_root=repo_root),
    )

    agents = SetupService(agent_factory=agent_factory).run(simulation_input=simulation_input)

    # TEST-mode agents are already connected via Kubernetes networking — skip Docker.
    all_test = all(a.mode == AgentMode.TEST for a in simulation_input.agents)
    if all_test:
        topology = NetworkTopology()
    else:
        topology = NetworkService(
            network_builder=DockerNetworkBuilder()
        ).run(simulation_input=simulation_input, agents=agents)

    if simulation_input.bootstrap is not None:
        DomainOntologyBootstrapService(agents=agents).bootstrap(
            simulation_input.bootstrap.domain_ontologies
        )

    # Synthesize a join event for every agent that declares a join_offset_time.
    # These are merged into the simulation timeline so the agent is provisioned
    # and registered at the correct simulated time.
    join_events = [
        AgentJoinEvent(time_offset=a.join_offset_time, agent=a)
        for a in simulation_input.agents
        if a.join_offset_time is not None
    ]

    fired_events = SimulationService(
        simulation_input=simulation_input,
        event_manager=EventManager(
            commands={
                "simulation": SimulationEventCommand(agents=agents, topology=topology),
                "agent_join": AgentJoinEventCommand(agents=agents, agent_factory=agent_factory),
                "partition": PartitionEventCommand(topology=topology),
            }
        ),
        clock_broadcast_service=ClockBroadcastService(agents=agents),
        extra_events=join_events,
    ).run()

    AnalysisService().run()

    EventDumpService(agents=agents).dump(
        simulation_name=simulation_input.name,
        fired_events=fired_events,
        output_dir=output_path,
        output_file_name=output_file_name,
    )
