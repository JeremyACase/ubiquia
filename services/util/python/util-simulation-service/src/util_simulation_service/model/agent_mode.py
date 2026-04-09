from enum import Enum


class AgentMode(Enum):
    """Enumerates the modes in which a Ubiquia agent can be installed by the simulation CLI."""

    MICROWEIGHT = "microweight"
    KIND = "KIND"
    TEST = "test"
