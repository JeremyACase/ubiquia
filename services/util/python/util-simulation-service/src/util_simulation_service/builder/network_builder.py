from abc import ABC, abstractmethod

from util_simulation_service.model.agent import Agent
from util_simulation_service.model.network import Network


class NetworkBuilder(ABC):
    """Abstract base for builders that connect a set of agents into a network."""

    @abstractmethod
    def build(self, network: Network, agents: list[Agent]) -> None:
        """Establish connectivity between the given agents within the named network."""
