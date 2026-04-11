from typing import Literal

from util_simulation_service.model.events.event import Event
from util_simulation_service.model.network import Network


class PartitionEvent(Event):
    """Redefines the network topology at a point in simulation time.

    When dispatched, the current topology is replaced with the networks declared
    here. Agents that share a network can communicate; agents in separate networks
    cannot.
    """

    type: Literal["partition"] = "partition"
    networks: list[Network]
