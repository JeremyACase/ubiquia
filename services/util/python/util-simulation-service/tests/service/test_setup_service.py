from unittest.mock import MagicMock

import pytest

from util_simulation_service.builder.agent_builder import AgentBuilder
from util_simulation_service.model.agent import Agent
from util_simulation_service.model.agent_mode import AgentMode
from util_simulation_service.model.simulation_event import SimulationEvent
from util_simulation_service.model.simulation_input import SimulationInput
from util_simulation_service.model.time_offset import TimeOffset
from util_simulation_service.service.agent_factory import AgentFactory
from util_simulation_service.service.setup_service import SetupService


def _make_simulation_input(agents: list[str]) -> SimulationInput:
    return SimulationInput(
        name="test-sim",
        agents=agents,
        events=[SimulationEvent(time_offset=TimeOffset(n=1.0), payload={})],
        networks=[],
        speed=1.0,
    )


def _make_factory(builder: AgentBuilder) -> AgentFactory:
    factory = MagicMock(spec=AgentFactory)
    factory.get_builder.return_value = builder
    return factory


class TestSetupService:
    def test_run_returns_one_agent_per_declared_agent(self):
        builder = MagicMock(spec=AgentBuilder)
        builder.build.side_effect = lambda name: Agent(name=name, base_url=f"http://{name}")
        service = SetupService(agent_factory=_make_factory(builder))

        agents = service.run(
            simulation_input=_make_simulation_input(["agent-a", "agent-b"]),
            mode=AgentMode.MICROWEIGHT,
        )

        assert len(agents) == 2
        assert agents[0].name == "agent-a"
        assert agents[1].name == "agent-b"

    def test_run_calls_factory_with_correct_mode(self):
        builder = MagicMock(spec=AgentBuilder)
        builder.build.return_value = Agent(name="a", base_url="http://a")
        factory = _make_factory(builder)
        service = SetupService(agent_factory=factory)

        service.run(
            simulation_input=_make_simulation_input(["a"]),
            mode=AgentMode.KIND,
        )

        factory.get_builder.assert_called_once_with(AgentMode.KIND)

    def test_run_calls_builder_for_each_agent(self):
        builder = MagicMock(spec=AgentBuilder)
        builder.build.side_effect = lambda name: Agent(name=name, base_url=f"http://{name}")
        service = SetupService(agent_factory=_make_factory(builder))

        service.run(
            simulation_input=_make_simulation_input(["agent-a", "agent-b", "agent-c"]),
            mode=AgentMode.MICROWEIGHT,
        )

        assert builder.build.call_count == 3
        builder.build.assert_any_call("agent-a")
        builder.build.assert_any_call("agent-b")
        builder.build.assert_any_call("agent-c")

    def test_run_returns_empty_list_for_no_agents(self):
        builder = MagicMock(spec=AgentBuilder)
        service = SetupService(agent_factory=_make_factory(builder))

        agents = service.run(
            simulation_input=_make_simulation_input([]),
            mode=AgentMode.MICROWEIGHT,
        )

        assert agents == []
        builder.build.assert_not_called()

    def test_run_propagates_builder_error(self):
        builder = MagicMock(spec=AgentBuilder)
        builder.build.side_effect = NotImplementedError
        service = SetupService(agent_factory=_make_factory(builder))

        with pytest.raises(NotImplementedError):
            service.run(
                simulation_input=_make_simulation_input(["agent-a"]),
                mode=AgentMode.MICROWEIGHT,
            )
