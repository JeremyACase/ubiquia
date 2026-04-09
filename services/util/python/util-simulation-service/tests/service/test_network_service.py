from unittest.mock import MagicMock, call

import pytest

from util_simulation_service.builder.network_builder import NetworkBuilder
from util_simulation_service.model.agent import Agent
from util_simulation_service.model.agent_input import AgentInput
from util_simulation_service.model.agent_mode import AgentMode
from util_simulation_service.model.network import Network
from util_simulation_service.model.network_topology import NetworkTopology
from util_simulation_service.model.events.simulation_event import SimulationEvent
from util_simulation_service.model.simulation_input import SimulationInput
from util_simulation_service.model.time_offset import TimeOffset
from util_simulation_service.service.network_service import NetworkService


def _agent(name: str) -> Agent:
    return Agent(name=name, base_url=f"http://localhost:8080")


def _agent_input(name: str) -> AgentInput:
    return AgentInput(name=name, mode=AgentMode.MICROWEIGHT)


def _make_simulation_input(agent_names: list[str], networks: list[Network]) -> SimulationInput:
    return SimulationInput(
        name="test-sim",
        agents=[_agent_input(n) for n in agent_names],
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

    def test_run_creates_default_network_when_none_defined(self):
        builder = MagicMock(spec=NetworkBuilder)
        sim = _make_simulation_input(["agent-a", "agent-b"], [])

        NetworkService(builder).run(sim, [_agent("agent-a"), _agent("agent-b")])

        builder.build.assert_called_once()
        passed_network = builder.build.call_args[0][0]
        assert passed_network.name == "default"

    def test_default_network_contains_all_agents(self):
        builder = MagicMock(spec=NetworkBuilder)
        sim = _make_simulation_input(["agent-a", "agent-b", "agent-c"], [])
        agents = [_agent("agent-a"), _agent("agent-b"), _agent("agent-c")]

        NetworkService(builder).run(sim, agents)

        passed_agents = builder.build.call_args[0][1]
        assert {a.name for a in passed_agents} == {"agent-a", "agent-b", "agent-c"}

    def test_run_propagates_builder_error(self):
        builder = MagicMock(spec=NetworkBuilder)
        builder.build.side_effect = RuntimeError("docker error")
        sim = _make_simulation_input(["agent-a"], [Network(name="net-1", agents=["agent-a"])])

        with pytest.raises(RuntimeError, match="docker error"):
            NetworkService(builder).run(sim, [_agent("agent-a")])

    def test_run_returns_topology(self):
        builder = MagicMock(spec=NetworkBuilder)
        network = Network(name="net-1", agents=["agent-a", "agent-b"])
        sim = _make_simulation_input(["agent-a", "agent-b"], [network])

        topology = NetworkService(builder).run(sim, [_agent("agent-a"), _agent("agent-b")])

        assert isinstance(topology, NetworkTopology)

    def test_default_network_topology_is_fully_connected(self):
        builder = MagicMock(spec=NetworkBuilder)
        sim = _make_simulation_input(["agent-a", "agent-b"], [])

        topology = NetworkService(builder).run(sim, [_agent("agent-a"), _agent("agent-b")])

        assert topology.can_reach("agent-a", "agent-b")
        assert topology.can_reach("agent-b", "agent-a")

    def test_topology_reflects_network_peers(self):
        builder = MagicMock(spec=NetworkBuilder)
        network = Network(name="net-1", agents=["agent-a", "agent-b", "agent-c"])
        agents = [_agent("agent-a"), _agent("agent-b"), _agent("agent-c")]
        sim = _make_simulation_input(["agent-a", "agent-b", "agent-c"], [network])

        topology = NetworkService(builder).run(sim, agents)

        assert topology.can_reach("agent-a", "agent-b")
        assert topology.can_reach("agent-a", "agent-c")
        assert not topology.can_reach("agent-a", "agent-a")

    def test_topology_does_not_connect_agents_in_separate_networks(self):
        builder = MagicMock(spec=NetworkBuilder)
        networks = [
            Network(name="net-1", agents=["agent-a", "agent-b"]),
            Network(name="net-2", agents=["agent-c"]),
        ]
        agents = [_agent("agent-a"), _agent("agent-b"), _agent("agent-c")]
        sim = _make_simulation_input(["agent-a", "agent-b", "agent-c"], networks)

        topology = NetworkService(builder).run(sim, agents)

        assert topology.can_reach("agent-a", "agent-b")
        assert not topology.can_reach("agent-a", "agent-c")
        assert not topology.can_reach("agent-b", "agent-c")

    def test_agent_in_multiple_networks_can_reach_peers_from_both(self):
        builder = MagicMock(spec=NetworkBuilder)
        networks = [
            Network(name="net-1", agents=["agent-a", "agent-b"]),
            Network(name="net-2", agents=["agent-a", "agent-c"]),
        ]
        agents = [_agent("agent-a"), _agent("agent-b"), _agent("agent-c")]
        sim = _make_simulation_input(["agent-a", "agent-b", "agent-c"], networks)

        topology = NetworkService(builder).run(sim, agents)

        assert topology.can_reach("agent-a", "agent-b")
        assert topology.can_reach("agent-a", "agent-c")
        assert not topology.can_reach("agent-b", "agent-c")
