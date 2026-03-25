from util_simulation_service.command.event_command import EventCommand
from util_simulation_service.model.event import Event
from util_simulation_service.model.simulation_event import SimulationEvent


class SimulationEventCommand(EventCommand):
    """Processes a SimulationEvent by dispatching its payload to the target agent."""

    def execute(self, event: Event) -> None:
        if not isinstance(event, SimulationEvent):
            raise TypeError(f"Expected SimulationEvent, got {type(event).__name__}")
        raise NotImplementedError
