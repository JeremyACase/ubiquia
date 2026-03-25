import pytest

from util_simulation_service.command.simulation_event_command import SimulationEventCommand
from util_simulation_service.model.event import Event
from util_simulation_service.model.simulation_event import SimulationEvent
from util_simulation_service.model.time_offset import TimeOffset


def _simulation_event(payload: dict | None = None) -> SimulationEvent:
    return SimulationEvent(time_offset=TimeOffset(n=1.0), payload=payload or {})


class TestSimulationEventCommand:
    def test_execute_raises_not_implemented_for_simulation_event(self):
        command = SimulationEventCommand()
        with pytest.raises(NotImplementedError):
            command.execute(_simulation_event())

    def test_execute_raises_type_error_for_wrong_event_type(self):
        class _OtherEvent(Event):
            type: str = "other"

        command = SimulationEventCommand()
        with pytest.raises(TypeError, match="Expected SimulationEvent"):
            command.execute(_OtherEvent(time_offset=TimeOffset(n=1.0)))
