import pytest
from unittest.mock import MagicMock

from util_simulation_service.command.event_command import EventCommand
from util_simulation_service.model.events.simulation_event import SimulationEvent
from util_simulation_service.model.time_offset import TimeOffset
from util_simulation_service.service.event_manager import EventManager


def _simulation_event() -> SimulationEvent:
    return SimulationEvent(time_offset=TimeOffset(n=1.0), target_agent="agent-a", endpoint="/bootstrap/ingest", payload={"key": "value"})


class TestEventManager:
    def test_dispatch_calls_registered_command(self):
        command = MagicMock(spec=EventCommand)
        manager = EventManager(commands={"simulation": command})
        event = _simulation_event()

        manager.dispatch(event)

        command.execute.assert_called_once_with(event)

    def test_dispatch_routes_by_event_type(self):
        sim_command = MagicMock(spec=EventCommand)
        other_command = MagicMock(spec=EventCommand)
        manager = EventManager(commands={"simulation": sim_command, "other": other_command})
        event = _simulation_event()

        manager.dispatch(event)

        sim_command.execute.assert_called_once_with(event)
        other_command.execute.assert_not_called()

    def test_dispatch_raises_for_unregistered_type(self):
        manager = EventManager(commands={})
        with pytest.raises(ValueError, match="No command registered for event type: 'simulation'"):
            manager.dispatch(_simulation_event())

    def test_dispatch_propagates_command_error(self):
        command = MagicMock(spec=EventCommand)
        command.execute.side_effect = NotImplementedError
        manager = EventManager(commands={"simulation": command})

        with pytest.raises(NotImplementedError):
            manager.dispatch(_simulation_event())
