from pydantic import BaseModel

from util_simulation_service.model.time_offset import TimeOffset


class Event(BaseModel):
    type: str
    time_offset: TimeOffset
