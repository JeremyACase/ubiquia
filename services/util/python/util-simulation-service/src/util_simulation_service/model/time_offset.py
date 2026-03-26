from enum import Enum

from pydantic import BaseModel, Field

_SECONDS_PER_UNIT: dict["TimeUnit", float] = {}


class TimeUnit(Enum):
    SECONDS = "seconds"
    MINUTES = "minutes"
    HOURS = "hours"


_SECONDS_PER_UNIT = {
    TimeUnit.SECONDS: 1.0,
    TimeUnit.MINUTES: 60.0,
    TimeUnit.HOURS: 3600.0,
}


class TimeOffset(BaseModel):
    n: float = Field(gt=0, description="Magnitude of the offset from simulation start.")
    unit: TimeUnit = TimeUnit.SECONDS

    def to_seconds(self) -> float:
        """Return the offset converted to seconds."""
        return self.n * _SECONDS_PER_UNIT[self.unit]
