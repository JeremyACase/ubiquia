import pytest
import httpx
import respx

from util_simulation_service.command.simulation_event_command import SimulationEventCommand
from util_simulation_service.model.agent import Agent
from util_simulation_service.model.events.event import Event
from util_simulation_service.model.events.simulation_event import SimulationEvent
from util_simulation_service.model.time_offset import TimeOffset


_BASE_URL = "http://localhost:9999"


def _agent(name: str = "agent-a") -> Agent:
    return Agent(name=name, base_url=_BASE_URL)


def _simulation_event(
    target_agent: str = "agent-a",
    endpoint: str = "/bootstrap/ingest",
    payload: dict | None = None,
) -> SimulationEvent:
    return SimulationEvent(
        time_offset=TimeOffset(n=1.0),
        target_agent=target_agent,
        endpoint=endpoint,
        payload=payload or {},
    )


class TestSimulationEventCommand:
    def test_execute_raises_type_error_for_wrong_event_type(self):
        class _OtherEvent(Event):
            type: str = "other"

        command = SimulationEventCommand()
        with pytest.raises(TypeError, match="Expected SimulationEvent"):
            command.execute(_OtherEvent(time_offset=TimeOffset(n=1.0)))

    def test_execute_skips_unknown_agent(self, caplog):
        command = SimulationEventCommand(agents=[_agent("agent-b")])
        command.execute(_simulation_event(target_agent="agent-x"))
        assert "agent-x" in caplog.text

    @respx.mock
    def test_execute_posts_payload_to_agent_endpoint(self):
        route = respx.post(f"{_BASE_URL}/bootstrap/ingest").mock(
            return_value=httpx.Response(200)
        )
        command = SimulationEventCommand(agents=[_agent()])
        command.execute(_simulation_event(payload={"colors": ["RED"]}))
        assert route.called
        assert route.calls[0].request.content == b'{"colors":["RED"]}'

    @respx.mock
    def test_execute_logs_error_on_http_failure(self, caplog):
        respx.post(f"{_BASE_URL}/bootstrap/ingest").mock(
            return_value=httpx.Response(500)
        )
        command = SimulationEventCommand(agents=[_agent()])
        command.execute(_simulation_event())
        assert "agent-a" in caplog.text
