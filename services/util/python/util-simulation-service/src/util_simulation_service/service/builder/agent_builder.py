from abc import ABC, abstractmethod

from util_simulation_service.model.agent import Agent


class AgentBuilder(ABC):
    """Abstract base for mode-specific Ubiquia agent builders."""

    @abstractmethod
    def build(self, agent_name: str) -> Agent:
        """Provision a Ubiquia agent and return its connection details."""
