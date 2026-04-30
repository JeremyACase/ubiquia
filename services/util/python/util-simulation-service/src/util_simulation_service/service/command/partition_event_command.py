import logging

from util_simulation_service.service.command.event_command import EventCommand
from util_simulation_service.model.events.event import Event
from util_simulation_service.model.events.partition_event import PartitionEvent
from util_simulation_service.model.network_topology import NetworkTopology

logger = logging.getLogger(__name__)


class PartitionEventCommand(EventCommand):
    """Applies a network partition by replacing the current topology.

    Clears all existing peer relationships and rebuilds them from the networks
    declared in the PartitionEvent. Agents that share a network can communicate;
    agents in separate networks cannot.
    """

    def __init__(self, topology: NetworkTopology) -> None:
        self._topology = topology

    def execute(self, event: Event) -> None:
        if not isinstance(event, PartitionEvent):
            raise TypeError(f"Expected PartitionEvent, got {type(event).__name__}")

        self._topology.reset()

        for network in event.networks:
            logger.info(
                "Partition: network '%s' contains agents %s.",
                network.name,
                network.agents,
            )
            for name in network.agents:
                for peer in network.agents:
                    if name != peer:
                        self._topology.add_peer(name, peer)

        logger.info("Partition applied: %d network(s) defined.", len(event.networks))
