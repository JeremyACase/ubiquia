import pytest

from util_simulation_service.command.partition_event_command import PartitionEventCommand
from util_simulation_service.model.events.partition_event import PartitionEvent
from util_simulation_service.model.events.simulation_event import SimulationEvent
from util_simulation_service.model.network import Network
from util_simulation_service.model.network_topology import NetworkTopology
from util_simulation_service.model.time_offset import TimeOffset


def _partition_event(*networks: Network) -> PartitionEvent:
    return PartitionEvent(time_offset=TimeOffset(n=60.0), networks=list(networks))


def _net(name: str, *agents: str) -> Network:
    return Network(name=name, agents=list(agents))


class TestPartitionEventCommand:
    def test_agents_in_same_network_can_reach_each_other(self):
        topology = NetworkTopology()
        command = PartitionEventCommand(topology=topology)

        command.execute(_partition_event(_net("net-a", "agent-a", "agent-b")))

        assert topology.can_reach("agent-a", "agent-b")
        assert topology.can_reach("agent-b", "agent-a")

    def test_agents_in_different_networks_cannot_reach_each_other(self):
        topology = NetworkTopology()
        command = PartitionEventCommand(topology=topology)

        command.execute(_partition_event(
            _net("net-a", "agent-a"),
            _net("net-b", "agent-b"),
        ))

        assert not topology.can_reach("agent-a", "agent-b")
        assert not topology.can_reach("agent-b", "agent-a")

    def test_partition_clears_previous_topology(self):
        topology = NetworkTopology()
        topology.add_peer("agent-a", "agent-b")
        topology.add_peer("agent-b", "agent-a")
        command = PartitionEventCommand(topology=topology)

        # Partition that isolates agent-a from agent-b
        command.execute(_partition_event(
            _net("net-a", "agent-a"),
            _net("net-b", "agent-b"),
        ))

        assert not topology.can_reach("agent-a", "agent-b")

    def test_second_partition_replaces_first(self):
        topology = NetworkTopology()
        command = PartitionEventCommand(topology=topology)

        command.execute(_partition_event(
            _net("net-a", "agent-a"),
            _net("net-b", "agent-b"),
        ))
        # Heal the partition
        command.execute(_partition_event(_net("net-all", "agent-a", "agent-b")))

        assert topology.can_reach("agent-a", "agent-b")
        assert topology.can_reach("agent-b", "agent-a")

    def test_agent_cannot_reach_itself(self):
        topology = NetworkTopology()
        command = PartitionEventCommand(topology=topology)

        command.execute(_partition_event(_net("net-a", "agent-a", "agent-b")))

        assert not topology.can_reach("agent-a", "agent-a")

    def test_agent_in_multiple_networks_can_reach_all_peers(self):
        topology = NetworkTopology()
        command = PartitionEventCommand(topology=topology)

        command.execute(_partition_event(
            _net("net-ab", "agent-a", "agent-b"),
            _net("net-ac", "agent-a", "agent-c"),
        ))

        assert topology.can_reach("agent-a", "agent-b")
        assert topology.can_reach("agent-a", "agent-c")
        assert not topology.can_reach("agent-b", "agent-c")

    def test_raises_on_wrong_event_type(self):
        topology = NetworkTopology()
        command = PartitionEventCommand(topology=topology)
        wrong_event = SimulationEvent(time_offset=TimeOffset(n=1.0), payload={})

        with pytest.raises(TypeError, match="Expected PartitionEvent"):
            command.execute(wrong_event)
