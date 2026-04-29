from unittest.mock import MagicMock

import pytest

from util_simulation_service.service.builder.agent_builder import AgentBuilder
from util_simulation_service.model.agent import Agent
from util_simulation_service.model.agent_input import AgentInput
from util_simulation_service.model.agent_mode import AgentMode
from util_simulation_service.model.events.simulation_event import SimulationEvent
from util_simulation_service.model.simulation_input import SimulationInput
from util_simulation_service.model.time_offset import TimeOffset
from util_simulation_service.service.factory.agent_factory import AgentFactory
from util_simulation_service.service.logic.pre_processing.setup_service import SetupService


def _agent_input(name: str, mode: AgentMode = AgentMode.MICROWEIGHT, deferred: bool = False) -> AgentInput:
    join_offset_time = TimeOffset(n=30.0) if deferred else None
    return AgentInput(name=name, mode=mode, join_offset_time=join_offset_time)


def _make_simulation_input(agent_inputs: list[AgentInput]) -> SimulationInput:
    return SimulationInput(
        name="test-sim",
        agents=agent_inputs,
        events=[SimulationEvent(time_offset=TimeOffset(n=1.0), target_agent="agent-a", endpoint="/bootstrap/ingest", payload={})],
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
            simulation_input=_make_simulation_input([
                _agent_input("agent-a"),
                _agent_input("agent-b"),
            ])
        )

        assert len(agents) == 2
        assert agents[0].name == "agent-a"
        assert agents[1].name == "agent-b"

    def test_run_calls_factory_with_per_agent_mode(self):
        builder = MagicMock(spec=AgentBuilder)
        builder.build.return_value = Agent(name="a", base_url="http://a")
        factory = _make_factory(builder)
        service = SetupService(agent_factory=factory)

        service.run(
            simulation_input=_make_simulation_input([_agent_input("a", AgentMode.KIND)])
        )

        factory.get_builder.assert_called_once_with(AgentMode.KIND)

    def test_run_calls_correct_builder_per_agent_mode(self):
        microweight_builder = MagicMock(spec=AgentBuilder)
        microweight_builder.build.return_value = Agent(name="a", base_url="http://a")
        kind_builder = MagicMock(spec=AgentBuilder)
        kind_builder.build.return_value = Agent(name="b", base_url="http://b")

        factory = MagicMock(spec=AgentFactory)
        factory.get_builder.side_effect = lambda mode: (
            microweight_builder if mode == AgentMode.MICROWEIGHT else kind_builder
        )

        service = SetupService(agent_factory=factory)
        service.run(
            simulation_input=_make_simulation_input([
                _agent_input("a", AgentMode.MICROWEIGHT),
                _agent_input("b", AgentMode.KIND),
            ])
        )

        microweight_builder.build.assert_called_once_with("a")
        kind_builder.build.assert_called_once_with("b")

    def test_run_calls_builder_for_each_agent(self):
        builder = MagicMock(spec=AgentBuilder)
        builder.build.side_effect = lambda name: Agent(name=name, base_url=f"http://{name}")
        service = SetupService(agent_factory=_make_factory(builder))

        service.run(
            simulation_input=_make_simulation_input([
                _agent_input("agent-a"),
                _agent_input("agent-b"),
                _agent_input("agent-c"),
            ])
        )

        assert builder.build.call_count == 3
        builder.build.assert_any_call("agent-a")
        builder.build.assert_any_call("agent-b")
        builder.build.assert_any_call("agent-c")

    def test_run_returns_empty_list_for_no_agents(self):
        builder = MagicMock(spec=AgentBuilder)
        service = SetupService(agent_factory=_make_factory(builder))

        agents = service.run(simulation_input=_make_simulation_input([]))

        assert agents == []
        builder.build.assert_not_called()

    def test_run_skips_deferred_agents(self):
        builder = MagicMock(spec=AgentBuilder)
        builder.build.side_effect = lambda name: Agent(name=name, base_url=f"http://{name}")
        service = SetupService(agent_factory=_make_factory(builder))

        agents = service.run(
            simulation_input=_make_simulation_input([
                _agent_input("agent-a"),
                _agent_input("agent-d", deferred=True),
            ])
        )

        assert len(agents) == 1
        assert agents[0].name == "agent-a"
        builder.build.assert_called_once_with("agent-a")

    def test_run_with_all_deferred_returns_empty_list(self):
        builder = MagicMock(spec=AgentBuilder)
        service = SetupService(agent_factory=_make_factory(builder))

        agents = service.run(
            simulation_input=_make_simulation_input([
                _agent_input("agent-a", deferred=True),
                _agent_input("agent-b", deferred=True),
            ])
        )

        assert agents == []
        builder.build.assert_not_called()

    def test_run_propagates_builder_error(self):
        builder = MagicMock(spec=AgentBuilder)
        builder.build.side_effect = NotImplementedError
        service = SetupService(agent_factory=_make_factory(builder))

        with pytest.raises(NotImplementedError):
            service.run(simulation_input=_make_simulation_input([_agent_input("agent-a")]))
