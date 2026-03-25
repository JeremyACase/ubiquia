from unittest.mock import MagicMock, call

import pytest

from util_simulation_service.builder.network_builder import NetworkBuilder
from util_simulation_service.model.agent import Agent
from util_simulation_service.model.event import Event
from util_simulation_service.model.network import Network
from util_simulation_service.model.simulation_event import SimulationEvent
from util_simulation_service.model.simulation_input import SimulationInput
from util_simulation_service.model.time_offset import TimeOffset
from util_simulation_service.service.network_service import NetworkService


def _agent(name: str) -> Agent:
    return Agent(name=name, base_url=f"http://localhost:8080")


def _make_simulation_input(agents: list[str], networks: list[Network]) -> SimulationInput:
    return SimulationInput(
        name="test-sim",
        agents=agents,
        events=[SimulationEvent(time_offset=TimeOffset(n=1.0), payload={})],
        networks=networks,
        speed=1.0,
    )


class TestNetworkService:
    def test_run_calls_builder_for_each_network(self):
        builder = MagicMock(spec=NetworkBuilder)
        networks = [
            Network(name="net-1", agents=["agent-a"]),
            Network(name="net-2", agents=["agent-b"]),
        ]
        agents = [_agent("agent-a"), _agent("agent-b")]
        sim = _make_simulation_input(["agent-a", "agent-b"], networks)

        NetworkService(builder).run(sim, agents)

        assert builder.build.call_count == 2

    def test_run_passes_correct_network_to_builder(self):
        builder = MagicMock(spec=NetworkBuilder)
        network = Network(name="net-1", agents=["agent-a"])
        sim = _make_simulation_input(["agent-a"], [network])

        NetworkService(builder).run(sim, [_agent("agent-a")])

        builder.build.assert_called_once()
        passed_network = builder.build.call_args[0][0]
        assert passed_network.name == "net-1"

    def test_run_resolves_agent_names_to_agent_objects(self):
        builder = MagicMock(spec=NetworkBuilder)
        network = Network(name="net-1", agents=["agent-a", "agent-b"])
        agents = [_agent("agent-a"), _agent("agent-b")]
        sim = _make_simulation_input(["agent-a", "agent-b"], [network])

        NetworkService(builder).run(sim, agents)

        passed_agents = builder.build.call_args[0][1]
        assert {a.name for a in passed_agents} == {"agent-a", "agent-b"}

    def test_run_only_passes_agents_in_that_network(self):
        builder = MagicMock(spec=NetworkBuilder)
        network = Network(name="net-1", agents=["agent-a"])
        agents = [_agent("agent-a"), _agent("agent-b")]
        sim = _make_simulation_input(["agent-a", "agent-b"], [network])

        NetworkService(builder).run(sim, agents)

        passed_agents = builder.build.call_args[0][1]
        assert len(passed_agents) == 1
        assert passed_agents[0].name == "agent-a"

    def test_run_skips_builder_when_no_networks(self):
        builder = MagicMock(spec=NetworkBuilder)
        sim = _make_simulation_input(["agent-a"], [])

        NetworkService(builder).run(sim, [_agent("agent-a")])

        builder.build.assert_not_called()

    def test_run_propagates_builder_error(self):
        builder = MagicMock(spec=NetworkBuilder)
        builder.build.side_effect = RuntimeError("docker error")
        sim = _make_simulation_input(["agent-a"], [Network(name="net-1", agents=["agent-a"])])

        with pytest.raises(RuntimeError, match="docker error"):
            NetworkService(builder).run(sim, [_agent("agent-a")])
