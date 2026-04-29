from unittest.mock import MagicMock

import pytest

from util_simulation_service.service.builder.agent_builder import AgentBuilder
from util_simulation_service.service.command.agent_join_event_command import AgentJoinEventCommand
from util_simulation_service.model.agent import Agent
from util_simulation_service.model.agent_input import AgentInput
from util_simulation_service.model.events.agent_join_event import AgentJoinEvent
from util_simulation_service.model.agent_mode import AgentMode
from util_simulation_service.model.events.simulation_event import SimulationEvent
from util_simulation_service.model.time_offset import TimeOffset
from util_simulation_service.service.factory.agent_factory import AgentFactory


def _join_event(name: str, mode: AgentMode = AgentMode.MICROWEIGHT, base_url: str | None = None) -> AgentJoinEvent:
    return AgentJoinEvent(
        time_offset=TimeOffset(n=30.0),
        agent=AgentInput(name=name, mode=mode, base_url=base_url),
    )


def _make_factory(builder: AgentBuilder) -> AgentFactory:
    factory = MagicMock(spec=AgentFactory)
    factory.get_builder.return_value = builder
    return factory


class TestAgentJoinEventCommand:
    def test_execute_appends_agent_to_list(self):
        builder = MagicMock(spec=AgentBuilder)
        builder.build.return_value = Agent(name="agent-d", base_url="http://agent-d")
        agents: list[Agent] = []
        command = AgentJoinEventCommand(agents=agents, agent_factory=_make_factory(builder))

        command.execute(_join_event("agent-d"))

        assert len(agents) == 1
        assert agents[0].name == "agent-d"

    def test_execute_test_mode_uses_base_url_directly(self):
        factory = MagicMock(spec=AgentFactory)
        agents: list[Agent] = []
        command = AgentJoinEventCommand(agents=agents, agent_factory=factory)

        command.execute(_join_event("agent-d", mode=AgentMode.TEST, base_url="http://agent-d:8080"))

        assert agents[0].base_url == "http://agent-d:8080"
        factory.get_builder.assert_not_called()

    def test_execute_non_test_mode_delegates_to_factory(self):
        builder = MagicMock(spec=AgentBuilder)
        builder.build.return_value = Agent(name="agent-d", base_url="http://agent-d")
        factory = _make_factory(builder)
        command = AgentJoinEventCommand(agents=[], agent_factory=factory)

        command.execute(_join_event("agent-d", mode=AgentMode.MICROWEIGHT))

        factory.get_builder.assert_called_once_with(AgentMode.MICROWEIGHT)
        builder.build.assert_called_once_with("agent-d")

    def test_execute_raises_on_wrong_event_type(self):
        command = AgentJoinEventCommand(agents=[], agent_factory=MagicMock(spec=AgentFactory))
        wrong_event = SimulationEvent(time_offset=TimeOffset(n=1.0), target_agent="agent-a", endpoint="/bootstrap/ingest", payload={})

        with pytest.raises(TypeError, match="Expected AgentJoinEvent"):
            command.execute(wrong_event)

    def test_execute_multiple_agents_all_appended(self):
        builder = MagicMock(spec=AgentBuilder)
        builder.build.side_effect = lambda name: Agent(name=name, base_url=f"http://{name}")
        agents: list[Agent] = []
        command = AgentJoinEventCommand(agents=agents, agent_factory=_make_factory(builder))

        command.execute(_join_event("agent-d"))
        command.execute(_join_event("agent-e"))

        assert [a.name for a in agents] == ["agent-d", "agent-e"]
