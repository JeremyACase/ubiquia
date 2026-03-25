import pytest
from pydantic import ValidationError

from util_simulation_service.model.network import Network
from util_simulation_service.model.simulation_event import SimulationEvent
from util_simulation_service.model.simulation_input import SimulationInput
from util_simulation_service.model.time_offset import TimeOffset, TimeUnit


def _make_event(n: float = 1.0, unit: TimeUnit = TimeUnit.SECONDS) -> SimulationEvent:
    return SimulationEvent(time_offset=TimeOffset(n=n, unit=unit), payload={"key": "value"})


def _make_valid_input(**overrides) -> dict:
    base = {
        "name": "test-sim",
        "agents": ["agent-a", "agent-b"],
        "events": [_make_event()],
        "networks": [Network(name="net-1", agents=["agent-a"])],
        "speed": 1.0,
    }
    base.update(overrides)
    return base


class TestSimulationInput:
    def test_valid_input_parses(self):
        data = _make_valid_input()
        sim = SimulationInput(**data)
        assert sim.name == "test-sim"
        assert sim.speed == 1.0

    def test_speed_must_be_positive(self):
        with pytest.raises(ValidationError):
            SimulationInput(**_make_valid_input(speed=0))

    def test_speed_must_be_positive_negative(self):
        with pytest.raises(ValidationError):
            SimulationInput(**_make_valid_input(speed=-1.0))

    def test_network_agent_not_in_agents_list_raises(self):
        unknown_network = Network(name="net-x", agents=["agent-unknown"])
        with pytest.raises(ValidationError, match="references agents not found"):
            SimulationInput(**_make_valid_input(networks=[unknown_network]))

    def test_network_agent_subset_of_agents_is_valid(self):
        network = Network(name="net-1", agents=["agent-b"])
        sim = SimulationInput(**_make_valid_input(networks=[network]))
        assert len(sim.networks) == 1

    def test_empty_networks_is_valid(self):
        sim = SimulationInput(**_make_valid_input(networks=[]))
        assert sim.networks == []

    def test_multiple_events(self):
        events = [_make_event(n=1.0), _make_event(n=2.0)]
        sim = SimulationInput(**_make_valid_input(events=events))
        assert len(sim.events) == 2

    def test_event_payload_preserved(self):
        event = SimulationEvent(
            time_offset=TimeOffset(n=1.0),
            payload={"foo": "bar", "count": 42},
        )
        sim = SimulationInput(**_make_valid_input(events=[event]))
        assert sim.events[0].payload == {"foo": "bar", "count": 42}

    def test_events_deserialize_to_correct_subtype(self):
        sim = SimulationInput.model_validate(
            {
                "name": "test-sim",
                "agents": ["agent-a"],
                "events": [
                    {
                        "type": "simulation",
                        "time_offset": {"n": 1.0, "unit": "seconds"},
                        "payload": {"x": 1},
                    }
                ],
                "networks": [],
                "speed": 1.0,
            }
        )
        assert isinstance(sim.events[0], SimulationEvent)
        assert sim.events[0].type == "simulation"

    def test_unknown_event_type_raises(self):
        with pytest.raises(ValidationError):
            SimulationInput.model_validate(
                {
                    "name": "test-sim",
                    "agents": ["agent-a"],
                    "events": [
                        {
                            "type": "unknown",
                            "time_offset": {"n": 1.0, "unit": "seconds"},
                        }
                    ],
                    "networks": [],
                    "speed": 1.0,
                }
            )
