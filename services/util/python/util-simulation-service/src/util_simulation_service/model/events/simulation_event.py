from typing import Any, Literal

from util_simulation_service.model.events.event import Event


class SimulationEvent(Event):
    type: Literal["simulation"] = "simulation"
    payload: dict[str, Any]
