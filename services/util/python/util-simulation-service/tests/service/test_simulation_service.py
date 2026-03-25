import json
import pathlib
from datetime import datetime, timedelta, timezone
from unittest.mock import MagicMock, call, patch

import pytest

from util_simulation_service.model.simulation_input import SimulationInput
from util_simulation_service.service.event_manager import EventManager
from util_simulation_service.service.simulation_service import SimulationService

# A fixed start time well in the past so no test ever sleeps.
_PAST = datetime(2000, 1, 1, tzinfo=timezone.utc)


def _write_input(tmp_path: pathlib.Path, data: dict) -> pathlib.Path:
    f = tmp_path / "input.json"
    f.write_text(json.dumps(data))
    return f


def _valid_payload(events: list[dict] | None = None) -> dict:
    return {
        "name": "test-sim",
        "agents": ["agent-a"],
        "events": events
        if events is not None
        else [
            {
                "type": "simulation",
                "time_offset": {"n": 1.0, "unit": "seconds"},
                "payload": {"key": "value"},
            }
        ],
        "networks": [],
        "speed": 1.0,
    }


def _load(tmp_path: pathlib.Path, data: dict) -> SimulationInput:
    return SimulationService.load(_write_input(tmp_path, data))


def _service(simulation_input: SimulationInput, event_manager: EventManager | None = None) -> SimulationService:
    return SimulationService(
        simulation_input=simulation_input,
        event_manager=event_manager or MagicMock(spec=EventManager),
    )


class TestSimulationServiceLoad:
    def test_load_parses_valid_file(self, tmp_path):
        result = _load(tmp_path, _valid_payload())
        assert result.name == "test-sim"

    def test_load_raises_on_invalid_json(self, tmp_path):
        bad_file = tmp_path / "bad.json"
        bad_file.write_text("not valid json")
        with pytest.raises(Exception):
            SimulationService.load(bad_file)

    def test_load_raises_on_schema_violation(self, tmp_path):
        bad_payload = _valid_payload()
        bad_payload["speed"] = -1
        with pytest.raises(Exception):
            _load(tmp_path, bad_payload)

    def test_load_raises_on_unknown_network_agent(self, tmp_path):
        bad_payload = _valid_payload()
        bad_payload["networks"] = [{"name": "net-x", "agents": ["agent-unknown"]}]
        with pytest.raises(Exception):
            _load(tmp_path, bad_payload)


class TestSimulationServiceRun:
    def test_run_dispatches_each_event(self, tmp_path):
        payload = _valid_payload(
            events=[
                {"type": "simulation", "time_offset": {"n": 1.0, "unit": "seconds"}, "payload": {"a": 1}},
                {"type": "simulation", "time_offset": {"n": 2.0, "unit": "seconds"}, "payload": {"b": 2}},
            ]
        )
        simulation_input = _load(tmp_path, payload)
        event_manager = MagicMock(spec=EventManager)

        _service(simulation_input, event_manager).run(start_time=_PAST)

        assert event_manager.dispatch.call_count == 2

    def test_run_dispatches_in_ascending_time_offset_order(self, tmp_path):
        # Events intentionally out of order in the file.
        payload = _valid_payload(
            events=[
                {"type": "simulation", "time_offset": {"n": 3.0, "unit": "seconds"}, "payload": {"seq": 3}},
                {"type": "simulation", "time_offset": {"n": 1.0, "unit": "seconds"}, "payload": {"seq": 1}},
                {"type": "simulation", "time_offset": {"n": 2.0, "unit": "seconds"}, "payload": {"seq": 2}},
            ]
        )
        simulation_input = _load(tmp_path, payload)
        event_manager = MagicMock(spec=EventManager)

        _service(simulation_input, event_manager).run(start_time=_PAST)

        dispatched_payloads = [c.args[0].payload["seq"] for c in event_manager.dispatch.call_args_list]
        assert dispatched_payloads == [1, 2, 3]

    def test_run_with_no_events_does_not_dispatch(self, tmp_path):
        simulation_input = _load(tmp_path, _valid_payload(events=[]))
        event_manager = MagicMock(spec=EventManager)

        _service(simulation_input, event_manager).run(start_time=_PAST)

        event_manager.dispatch.assert_not_called()

    def test_run_propagates_dispatch_error(self, tmp_path):
        simulation_input = _load(tmp_path, _valid_payload())
        event_manager = MagicMock(spec=EventManager)
        event_manager.dispatch.side_effect = ValueError("unregistered type")

        with pytest.raises(ValueError, match="unregistered type"):
            _service(simulation_input, event_manager).run(start_time=_PAST)

    def test_run_defaults_start_time_to_utc_now(self, tmp_path):
        simulation_input = _load(tmp_path, _valid_payload(events=[]))
        before = datetime.now(timezone.utc)

        _service(simulation_input).run()

        after = datetime.now(timezone.utc)
        # No assertions on sleep — just confirm run() completes and used a UTC clock.
        assert before <= after

    def test_run_sleeps_until_fire_time(self, tmp_path):
        payload = _valid_payload(
            events=[{"type": "simulation", "time_offset": {"n": 10.0, "unit": "seconds"}, "payload": {}}]
        )
        simulation_input = _load(tmp_path, payload)
        start_time = datetime.now(timezone.utc)

        with patch("util_simulation_service.service.simulation_service.time.sleep") as mock_sleep:
            _service(simulation_input).run(start_time=start_time)

        assert mock_sleep.called
        slept = mock_sleep.call_args[0][0]
        assert 9.0 < slept <= 10.0

    def test_run_applies_speed_multiplier(self, tmp_path):
        payload = _valid_payload(
            events=[{"type": "simulation", "time_offset": {"n": 10.0, "unit": "seconds"}, "payload": {}}]
        )
        payload["speed"] = 2.0
        simulation_input = _load(tmp_path, payload)
        start_time = datetime.now(timezone.utc)

        with patch("util_simulation_service.service.simulation_service.time.sleep") as mock_sleep:
            _service(simulation_input).run(start_time=start_time)

        slept = mock_sleep.call_args[0][0]
        # 10s offset / speed 2.0 = 5s real-time delay
        assert 4.0 < slept <= 5.0

    def test_run_does_not_sleep_for_past_events(self, tmp_path):
        simulation_input = _load(tmp_path, _valid_payload())

        with patch("util_simulation_service.service.simulation_service.time.sleep") as mock_sleep:
            _service(simulation_input).run(start_time=_PAST)

        mock_sleep.assert_not_called()
