import pytest

from util_simulation_service.builder.agent_builder import AgentBuilder
from util_simulation_service.model.agent import Agent
from util_simulation_service.model.agent_mode import AgentMode
from util_simulation_service.service.agent_factory import AgentFactory


def _stub_builder(base_url: str) -> AgentBuilder:
    class _Stub(AgentBuilder):
        def build(self, agent_name: str) -> Agent:
            return Agent(name=agent_name, base_url=base_url)

    return _Stub()


class TestAgentFactory:
    def setup_method(self):
        self.microweight_builder = _stub_builder("http://microweight")
        self.kind_builder = _stub_builder("http://kind")
        self.factory = AgentFactory(
            microweight_builder=self.microweight_builder,
            kind_builder=self.kind_builder,
        )

    def test_get_builder_returns_microweight_builder(self):
        assert self.factory.get_builder(AgentMode.MICROWEIGHT) is self.microweight_builder

    def test_get_builder_returns_kind_builder(self):
        assert self.factory.get_builder(AgentMode.KIND) is self.kind_builder

    def test_get_builder_raises_for_unregistered_mode(self):
        # Simulate an unregistered mode by removing an entry directly
        factory = AgentFactory(
            microweight_builder=self.microweight_builder,
            kind_builder=self.kind_builder,
        )
        factory._builders.pop(AgentMode.KIND)

        with pytest.raises(ValueError, match="No builder registered"):
            factory.get_builder(AgentMode.KIND)
