from typing import Annotated, Union

from pydantic import BaseModel, Field, model_validator

from util_simulation_service.model.network import Network
from util_simulation_service.model.simulation_event import SimulationEvent

AnyEvent = Annotated[Union[SimulationEvent], Field(discriminator="type")]


class SimulationInput(BaseModel):
    name: str
    agents: list[str]
    events: list[AnyEvent]
    networks: list[Network]
    speed: float = Field(gt=0, description="Playback speed multiplier (e.g. 2.0 = twice real-time).")

    @model_validator(mode="after")
    def validate_network_agents(self) -> "SimulationInput":
        known = set(self.agents)
        for network in self.networks:
            unknown = [a for a in network.agents if a not in known]
            if unknown:
                raise ValueError(
                    f"Network '{network.name}' references agents not found in the agents list: {unknown}"
                )
        return self
