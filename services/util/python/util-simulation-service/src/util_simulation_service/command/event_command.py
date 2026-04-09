from abc import ABC, abstractmethod

from util_simulation_service.model.events.event import Event


class EventCommand(ABC):
    """Abstract base for commands that process a specific type of simulation event."""

    @abstractmethod
    def execute(self, event: Event) -> None:
        """Process the event."""
