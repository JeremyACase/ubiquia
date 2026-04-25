import pathlib
from datetime import datetime, timedelta, timezone
from unittest.mock import MagicMock, call, patch

import pytest
import yaml

from util_simulation_service.model.agent_input import AgentInput
from util_simulation_service.model.events.agent_join_event import AgentJoinEvent
from util_simulation_service.model.agent_mode import AgentMode
from util_simulation_service.model.simulation_input import SimulationInput
from util_simulation_service.model.time_offset import TimeOffset
from util_simulation_service.service.clock_broadcast_service import ClockBroadcastService
from util_simulation_service.service.event_manager import EventManager
from util_simulation_service.service.simulation_service import SimulationService

# A fixed start time well in the past so no test ever sleeps.
_PAST = datetime(2000, 1, 1, tzinfo=timezone.utc)


def _write_input(tmp_path: pathlib.Path, data: dict) -> pathlib.Path:
    f = tmp_path / "input.yaml"
    f.write_text(yaml.dump(data))
    return f


def _sim_event(n: float, payload: dict | None = None) -> dict:
    return {
        "type": "simulation",
        "time_offset": {"n": n, "unit": "seconds"},
        "target_agent": "agent-a",
        "endpoint": "/bootstrap/ingest",
        "payload": payload or {},
    }


def _valid_payload(events: list[dict] | None = None) -> dict:
    return {
        "name": "test-sim",
        "agents": [{"name": "agent-a", "mode": "microweight"}],
        "events": events if events is not None else [_sim_event(1.0, {"key": "value"})],
        "networks": [],
        "speed": 1.0,
    }


def _load(tmp_path: pathlib.Path, data: dict) -> SimulationInput:
    return SimulationService.load(_write_input(tmp_path, data))


def _service(
    simulation_input: SimulationInput,
    event_manager: EventManager | None = None,
    clock_broadcast_service: ClockBroadcastService | None = None,
) -> SimulationService:
    return SimulationService(
        simulation_input=simulation_input,
        event_manager=event_manager or MagicMock(spec=EventManager),
        clock_broadcast_service=clock_broadcast_service,
    )


class TestSimulationServiceLoad:
    def test_load_parses_valid_file(self, tmp_path):
        result = _load(tmp_path, _valid_payload())
        assert result.name == "test-sim"

    def test_load_raises_on_invalid_yaml(self, tmp_path):
        bad_file = tmp_path / "bad.yaml"
        bad_file.write_text("{")
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
                _sim_event(1.0, {"a": 1}),
                _sim_event(2.0, {"b": 2}),
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
                _sim_event(3.0, {"seq": 3}),
                _sim_event(1.0, {"seq": 1}),
                _sim_event(2.0, {"seq": 2}),
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
        payload = _valid_payload(events=[_sim_event(10.0)])
        simulation_input = _load(tmp_path, payload)
        start_time = datetime.now(timezone.utc)

        with patch("util_simulation_service.service.simulation_service.time.sleep") as mock_sleep:
            _service(simulation_input).run(start_time=start_time)

        assert mock_sleep.called
        slept = mock_sleep.call_args[0][0]
        assert 9.0 < slept <= 10.0

    def test_run_applies_speed_multiplier(self, tmp_path):
        payload = _valid_payload(events=[_sim_event(10.0)])
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


class TestSimulationServiceExtraEvents:
    def test_extra_events_are_dispatched(self, tmp_path):
        simulation_input = _load(tmp_path, _valid_payload(events=[]))
        event_manager = MagicMock(spec=EventManager)
        join_event = AgentJoinEvent(
            time_offset=TimeOffset(n=1.0),
            agent=AgentInput(name="agent-d", mode=AgentMode.MICROWEIGHT),
        )

        SimulationService(
            simulation_input=simulation_input,
            event_manager=event_manager,
            extra_events=[join_event],
        ).run(start_time=_PAST)

        event_manager.dispatch.assert_called_once_with(join_event)

    def test_extra_events_sorted_with_scenario_events(self, tmp_path):
        payload = _valid_payload(
            events=[
                _sim_event(3.0, {"seq": 3}),
                _sim_event(1.0, {"seq": 1}),
            ]
        )
        simulation_input = _load(tmp_path, payload)
        event_manager = MagicMock(spec=EventManager)
        join_event = AgentJoinEvent(
            time_offset=TimeOffset(n=2.0),
            agent=AgentInput(name="agent-d", mode=AgentMode.MICROWEIGHT),
        )

        SimulationService(
            simulation_input=simulation_input,
            event_manager=event_manager,
            extra_events=[join_event],
        ).run(start_time=_PAST)

        assert event_manager.dispatch.call_count == 3
        first, second, third = [c.args[0] for c in event_manager.dispatch.call_args_list]
        assert first.time_offset.to_seconds() == 1.0
        assert second is join_event
        assert third.time_offset.to_seconds() == 3.0

    def test_no_extra_events_behaves_as_before(self, tmp_path):
        simulation_input = _load(tmp_path, _valid_payload())
        event_manager = MagicMock(spec=EventManager)

        SimulationService(
            simulation_input=simulation_input,
            event_manager=event_manager,
        ).run(start_time=_PAST)

        assert event_manager.dispatch.call_count == 1


class TestSimulationServiceRunReturnValue:
    def test_run_returns_list(self, tmp_path):
        simulation_input = _load(tmp_path, _valid_payload())
        result = _service(simulation_input).run(start_time=_PAST)
        assert isinstance(result, list)

    def test_run_returns_one_record_per_event(self, tmp_path):
        payload = _valid_payload(
            events=[
                _sim_event(1.0),
                _sim_event(2.0),
            ]
        )
        simulation_input = _load(tmp_path, payload)
        result = _service(simulation_input).run(start_time=_PAST)
        assert len(result) == 2

    def test_run_records_contain_expected_keys(self, tmp_path):
        simulation_input = _load(tmp_path, _valid_payload())
        result = _service(simulation_input).run(start_time=_PAST)
        record = result[0]
        assert record["source"] == "simulation"
        assert record["type"] == "simulation"
        assert "time_offset_seconds" in record
        assert "fired_at" in record
        assert "details" in record
        assert "_sort_time" in record

    def test_run_records_time_offset_seconds(self, tmp_path):
        payload = _valid_payload(events=[_sim_event(5.0)])
        simulation_input = _load(tmp_path, payload)
        result = _service(simulation_input).run(start_time=_PAST)
        assert result[0]["time_offset_seconds"] == 5.0

    def test_run_returns_empty_list_when_no_events(self, tmp_path):
        simulation_input = _load(tmp_path, _valid_payload(events=[]))
        result = _service(simulation_input).run(start_time=_PAST)
        assert result == []

    def test_run_records_sort_time_matches_fired_at(self, tmp_path):
        simulation_input = _load(tmp_path, _valid_payload())
        result = _service(simulation_input).run(start_time=_PAST)
        record = result[0]
        assert record["_sort_time"] == record["fired_at"]

    def test_run_extra_events_included_in_return(self, tmp_path):
        simulation_input = _load(tmp_path, _valid_payload(events=[]))
        join_event = AgentJoinEvent(
            time_offset=TimeOffset(n=1.0),
            agent=AgentInput(name="agent-d", mode=AgentMode.MICROWEIGHT),
        )
        result = SimulationService(
            simulation_input=simulation_input,
            event_manager=MagicMock(spec=EventManager),
            extra_events=[join_event],
        ).run(start_time=_PAST)
        assert len(result) == 1
        assert result[0]["type"] == "agent_join"


class TestSimulationServiceClockBroadcast:
    def test_broadcast_called_once_per_event_when_speed_is_not_one(self, tmp_path):
        payload = _valid_payload(
            events=[
                _sim_event(1.0),
                _sim_event(2.0),
            ]
        )
        payload["speed"] = 2.0
        simulation_input = _load(tmp_path, payload)
        broadcaster = MagicMock(spec=ClockBroadcastService)

        _service(simulation_input, clock_broadcast_service=broadcaster).run(start_time=_PAST)

        assert broadcaster.broadcast.call_count == 2

    def test_broadcast_not_called_when_speed_is_one(self, tmp_path):
        simulation_input = _load(tmp_path, _valid_payload())
        broadcaster = MagicMock(spec=ClockBroadcastService)

        _service(simulation_input, clock_broadcast_service=broadcaster).run(start_time=_PAST)

        broadcaster.broadcast.assert_not_called()

    def test_broadcast_not_called_when_no_broadcaster_provided(self, tmp_path):
        payload = _valid_payload()
        payload["speed"] = 5.0
        simulation_input = _load(tmp_path, payload)

        # Should complete without error — no broadcaster wired in
        _service(simulation_input).run(start_time=_PAST)

    def test_broadcast_receives_simulated_time_not_wall_clock_time(self, tmp_path):
        offset_seconds = 30.0
        payload = _valid_payload(events=[_sim_event(offset_seconds)])
        payload["speed"] = 10.0
        simulation_input = _load(tmp_path, payload)
        broadcaster = MagicMock(spec=ClockBroadcastService)
        start_time = datetime(2025, 1, 1, 0, 0, 0, tzinfo=timezone.utc)

        _service(simulation_input, clock_broadcast_service=broadcaster).run(start_time=start_time)

        broadcast_time = broadcaster.broadcast.call_args[0][0]
        expected = start_time + timedelta(seconds=offset_seconds)
        assert broadcast_time == expected

    def test_broadcast_called_before_dispatch(self, tmp_path):
        payload = _valid_payload()
        payload["speed"] = 2.0
        simulation_input = _load(tmp_path, payload)
        broadcaster = MagicMock(spec=ClockBroadcastService)
        event_manager = MagicMock(spec=EventManager)
        call_order = []
        broadcaster.broadcast.side_effect = lambda *_: call_order.append("broadcast")
        event_manager.dispatch.side_effect = lambda *_: call_order.append("dispatch")

        _service(simulation_input, event_manager, broadcaster).run(start_time=_PAST)

        assert call_order == ["broadcast", "dispatch"]
