from typing import Annotated, Union

from pydantic import BaseModel, Field, model_validator

from util_simulation_service.model.agent_input import AgentInput
from util_simulation_service.model.bootstrap_input import BootstrapInput
from util_simulation_service.model.network import Network
from util_simulation_service.model.events.partition_event import PartitionEvent
from util_simulation_service.model.events.simulation_event import SimulationEvent
from util_simulation_service.model.time_offset import TimeOffset

AnyEvent = Annotated[Union[SimulationEvent, PartitionEvent], Field(discriminator="type")]


class SimulationInput(BaseModel):
    name: str
    agents: list[AgentInput]
    bootstrap: BootstrapInput | None = None
    events: list[AnyEvent]
    networks: list[Network]
    speed: float = Field(gt=0, description="Playback speed multiplier (e.g. 2.0 = twice real-time).")
    duration: TimeOffset | None = Field(default=None, description="Total scenario duration. All event offsets must fall within this window.")

    @model_validator(mode="after")
    def validate_network_agents(self) -> "SimulationInput":
        known = {a.name for a in self.agents}
        for network in self.networks:
            unknown = [a for a in network.agents if a not in known]
            if unknown:
                raise ValueError(
                    f"Network '{network.name}' references agents not found in the agents list: {unknown}"
                )
        return self

    @model_validator(mode="after")
    def validate_partition_event_agents(self) -> "SimulationInput":
        known = {a.name for a in self.agents}
        for event in self.events:
            if not isinstance(event, PartitionEvent):
                continue
            for network in event.networks:
                unknown = [a for a in network.agents if a not in known]
                if unknown:
                    raise ValueError(
                        f"Partition event network '{network.name}' references agents not found "
                        f"in the agents list: {unknown}"
                    )
        return self

    @model_validator(mode="after")
    def validate_bootstrap_targets(self) -> "SimulationInput":
        if self.bootstrap is None:
            return self
        known = {a.name for a in self.agents}
        for entry in self.bootstrap.domain_ontologies:
            unknown = [t for t in entry.targets if t not in known]
            if unknown:
                raise ValueError(
                    f"Domain ontology '{entry.file}' references agents not found in the agents list: {unknown}"
                )
        return self

