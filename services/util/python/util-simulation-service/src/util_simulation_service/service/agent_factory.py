from util_simulation_service.builder.agent_builder import AgentBuilder
from util_simulation_service.builder.kind_agent_builder import KindAgentBuilder
from util_simulation_service.builder.microweight_agent_builder import MicroweightAgentBuilder
from util_simulation_service.model.agent_mode import AgentMode


class AgentFactory:
    """Resolves the correct AgentBuilder for a given AgentMode."""

    def __init__(
        self,
        microweight_builder: MicroweightAgentBuilder,
        kind_builder: KindAgentBuilder,
    ):
        self._builders: dict[AgentMode, AgentBuilder] = {
            AgentMode.MICROWEIGHT: microweight_builder,
            AgentMode.KIND: kind_builder,
        }

    def get_builder(self, mode: AgentMode) -> AgentBuilder:
        builder = self._builders.get(mode)
        if builder is None:
            raise ValueError(f"No builder registered for mode: {mode}")
        return builder
