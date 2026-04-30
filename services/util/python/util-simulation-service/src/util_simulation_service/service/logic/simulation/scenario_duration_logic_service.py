import logging

from util_simulation_service.model.simulation_input import SimulationInput

logger = logging.getLogger(__name__)

_DEFAULT_DURATION_SECONDS = 60.0
_TRAILING_BUFFER_SECONDS = 5.0


class ScenarioDurationLogicService:
    """Resolves the effective run duration for a scenario.

    Priority:
      1. User-defined ``duration`` field — used as-is; a warning is emitted for
         every event whose offset exceeds it, since those events will not complete.
      2. Last event offset + 5-second trailing buffer — when no duration is set
         but events exist.
      3. 1-minute default — when neither a duration nor any events are defined.
    """

    def resolve(self, simulation_input: SimulationInput) -> float:
        """Return the effective scenario duration in seconds."""
        if simulation_input.duration is not None:
            return self._resolve_from_user_duration(simulation_input)

        if simulation_input.events:
            return self._resolve_from_last_event(simulation_input)

        logger.info(
            "Scenario '%s': no duration or events defined — defaulting to %.1fs.",
            simulation_input.name,
            _DEFAULT_DURATION_SECONDS,
        )
        return _DEFAULT_DURATION_SECONDS

    def _resolve_from_user_duration(self, simulation_input: SimulationInput) -> float:
        resolved = simulation_input.duration.to_seconds()
        for event in simulation_input.events:
            offset = event.time_offset.to_seconds()
            if offset > resolved:
                logger.warning(
                    "Scenario '%s': event of type '%s' at offset %.1fs exceeds the defined "
                    "duration of %.1fs — this event will not complete.",
                    simulation_input.name,
                    event.type,
                    offset,
                    resolved,
                )
        return resolved

    def _resolve_from_last_event(self, simulation_input: SimulationInput) -> float:
        last_offset = max(e.time_offset.to_seconds() for e in simulation_input.events)
        resolved = last_offset + _TRAILING_BUFFER_SECONDS
        logger.debug(
            "No duration defined for scenario '%s'; using last event offset (%.1fs) "
            "+ %.1fs trailing buffer = %.1fs.",
            simulation_input.name,
            last_offset,
            _TRAILING_BUFFER_SECONDS,
            resolved,
        )
        return resolved
