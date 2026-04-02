import json
import logging
import pathlib
import time
from datetime import datetime, timedelta, timezone

from util_simulation_service.model.simulation_input import SimulationInput
from util_simulation_service.service.clock_broadcast_service import ClockBroadcastService
from util_simulation_service.service.event_manager import EventManager

logger = logging.getLogger(__name__)


class SimulationService:
    """Drives the main simulation loop.

    Iterates over every event in the SimulationInput and delegates processing
    to the EventManager, which routes each event to its registered command.

    When a ClockBroadcastService is provided and the simulation speed is not
    1.0, the simulated wall-clock time is broadcast to every core service on
    every agent before each event is dispatched.
    """

    def __init__(
        self,
        simulation_input: SimulationInput,
        event_manager: EventManager,
        clock_broadcast_service: ClockBroadcastService | None = None,
    ):
        self._simulation_input = simulation_input
        self._event_manager = event_manager
        self._clock_broadcast_service = clock_broadcast_service

    @staticmethod
    def load(input_file: pathlib.Path) -> SimulationInput:
        return SimulationInput.model_validate(json.loads(input_file.read_text()))

    def run(self, start_time: datetime | None = None) -> None:
        if start_time is None:
            start_time = datetime.now(timezone.utc)

        speed = self._simulation_input.speed
        sorted_events = sorted(
            self._simulation_input.events,
            key=lambda e: e.time_offset.to_seconds(),
        )

        logger.info(
            "Starting simulation '%s': %d event(s) at %.1fx speed.",
            self._simulation_input.name,
            len(sorted_events),
            speed,
        )

        for i, event in enumerate(sorted_events, start=1):
            fire_at = start_time + timedelta(seconds=event.time_offset.to_seconds() / speed)
            delay = (fire_at - datetime.now(timezone.utc)).total_seconds()

            if delay > 0:
                logger.debug(
                    "Event %d/%d: waiting %.2fs (type=%s, offset=%ss).",
                    i, len(sorted_events), delay, event.type, event.time_offset.to_seconds(),
                )
                time.sleep(delay)

            if self._clock_broadcast_service is not None and speed != 1.0:
                simulated_time = start_time + timedelta(seconds=event.time_offset.to_seconds())
                self._clock_broadcast_service.broadcast(simulated_time)

            logger.info(
                "Event %d/%d: dispatching type=%s at offset=%ss.",
                i, len(sorted_events), event.type, event.time_offset.to_seconds(),
            )
            self._event_manager.dispatch(event)

        logger.info("Simulation '%s' complete.", self._simulation_input.name)
