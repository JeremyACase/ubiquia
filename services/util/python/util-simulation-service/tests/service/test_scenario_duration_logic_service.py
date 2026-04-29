import logging

import pytest

from util_simulation_service.model.agent_input import AgentInput
from util_simulation_service.model.agent_mode import AgentMode
from util_simulation_service.model.events.simulation_event import SimulationEvent
from util_simulation_service.model.simulation_input import SimulationInput
from util_simulation_service.model.time_offset import TimeOffset, TimeUnit
from util_simulation_service.service.logic.simulation.scenario_duration_logic_service import ScenarioDurationLogicService


def _agent() -> AgentInput:
    return AgentInput(name="agent-a", mode=AgentMode.MICROWEIGHT)


def _event(offset_seconds: float) -> SimulationEvent:
    return SimulationEvent(
        time_offset=TimeOffset(n=offset_seconds, unit=TimeUnit.SECONDS),
        target_agent="agent-a",
        endpoint="/ingest",
        payload={},
    )


def _sim(
    events: list[SimulationEvent] | None = None,
    duration: TimeOffset | None = None,
) -> SimulationInput:
    return SimulationInput(
        name="test-sim",
        agents=[_agent()],
        events=events or [],
        networks=[],
        speed=1.0,
        duration=duration,
    )


_SERVICE = ScenarioDurationLogicService()


class TestDefaultDuration:
    def test_returns_sixty_seconds_when_no_duration_and_no_events(self):
        result = _SERVICE.resolve(_sim())
        assert result == 60.0

    def test_default_is_independent_of_scenario_name(self):
        sim = SimulationInput(
            name="other-scenario",
            agents=[_agent()],
            events=[],
            networks=[],
            speed=1.0,
        )
        assert _SERVICE.resolve(sim) == 60.0

    def test_default_logs_info_to_console(self, caplog):
        with caplog.at_level(logging.INFO):
            _SERVICE.resolve(_sim())

        assert any(r.levelno == logging.INFO for r in caplog.records)

    def test_default_log_message_includes_scenario_name(self, caplog):
        with caplog.at_level(logging.INFO):
            _SERVICE.resolve(_sim())

        assert any("test-sim" in r.message for r in caplog.records)


class TestLastEventDuration:
    def test_single_event_adds_trailing_buffer(self):
        result = _SERVICE.resolve(_sim(events=[_event(10.0)]))
        assert result == 15.0

    def test_uses_max_offset_across_multiple_events(self):
        result = _SERVICE.resolve(_sim(events=[_event(5.0), _event(30.0), _event(15.0)]))
        assert result == 35.0

    def test_unsorted_events_still_use_max_offset(self):
        result = _SERVICE.resolve(_sim(events=[_event(20.0), _event(3.0), _event(50.0)]))
        assert result == 55.0

    def test_trailing_buffer_is_five_seconds(self):
        result = _SERVICE.resolve(_sim(events=[_event(100.0)]))
        assert result == 105.0

    def test_single_event_at_one_second(self):
        result = _SERVICE.resolve(_sim(events=[_event(1.0)]))
        assert result == 6.0


class TestUserDefinedDuration:
    def test_returns_exact_user_defined_seconds(self):
        duration = TimeOffset(n=120.0, unit=TimeUnit.SECONDS)
        result = _SERVICE.resolve(_sim(duration=duration))
        assert result == 120.0

    def test_converts_minutes_to_seconds(self):
        duration = TimeOffset(n=2.0, unit=TimeUnit.MINUTES)
        result = _SERVICE.resolve(_sim(duration=duration))
        assert result == 120.0

    def test_converts_hours_to_seconds(self):
        duration = TimeOffset(n=1.0, unit=TimeUnit.HOURS)
        result = _SERVICE.resolve(_sim(duration=duration))
        assert result == 3600.0

    def test_user_duration_takes_precedence_over_last_event(self):
        duration = TimeOffset(n=200.0, unit=TimeUnit.SECONDS)
        result = _SERVICE.resolve(_sim(events=[_event(50.0)], duration=duration))
        assert result == 200.0

    def test_user_duration_with_no_events(self):
        duration = TimeOffset(n=45.0, unit=TimeUnit.SECONDS)
        result = _SERVICE.resolve(_sim(duration=duration))
        assert result == 45.0


class TestOverdueEventWarnings:
    def test_no_warning_when_all_events_within_duration(self, caplog):
        duration = TimeOffset(n=60.0, unit=TimeUnit.SECONDS)
        sim = _sim(events=[_event(10.0), _event(30.0)], duration=duration)

        with caplog.at_level(logging.WARNING):
            _SERVICE.resolve(sim)

        assert not caplog.records

    def test_no_warning_when_event_exactly_at_duration_boundary(self, caplog):
        duration = TimeOffset(n=30.0, unit=TimeUnit.SECONDS)
        sim = _sim(events=[_event(30.0)], duration=duration)

        with caplog.at_level(logging.WARNING):
            _SERVICE.resolve(sim)

        assert not caplog.records

    def test_warning_emitted_when_event_exceeds_duration(self, caplog):
        duration = TimeOffset(n=10.0, unit=TimeUnit.SECONDS)
        sim = _sim(events=[_event(20.0)], duration=duration)

        with caplog.at_level(logging.WARNING):
            _SERVICE.resolve(sim)

        assert len(caplog.records) == 1
        assert "20.0" in caplog.records[0].message
        assert "10.0" in caplog.records[0].message

    def test_one_warning_per_exceeding_event(self, caplog):
        duration = TimeOffset(n=10.0, unit=TimeUnit.SECONDS)
        sim = _sim(events=[_event(5.0), _event(15.0), _event(25.0)], duration=duration)

        with caplog.at_level(logging.WARNING):
            _SERVICE.resolve(sim)

        assert len(caplog.records) == 2

    def test_warning_includes_scenario_name(self, caplog):
        duration = TimeOffset(n=5.0, unit=TimeUnit.SECONDS)
        sim = _sim(events=[_event(10.0)], duration=duration)

        with caplog.at_level(logging.WARNING):
            _SERVICE.resolve(sim)

        assert "test-sim" in caplog.records[0].message

    def test_no_warning_when_duration_set_but_no_events(self, caplog):
        duration = TimeOffset(n=30.0, unit=TimeUnit.SECONDS)
        sim = _sim(duration=duration)

        with caplog.at_level(logging.WARNING):
            _SERVICE.resolve(sim)

        assert not caplog.records
