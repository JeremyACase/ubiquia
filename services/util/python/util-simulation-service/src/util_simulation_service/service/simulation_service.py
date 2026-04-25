import logging
import pathlib
import time

import yaml
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
        extra_events: list | None = None,
    ):
        self._simulation_input = simulation_input
        self._event_manager = event_manager
        self._clock_broadcast_service = clock_broadcast_service
        self._extra_events = extra_events or []

    @staticmethod
    def load(input_file: pathlib.Path) -> SimulationInput:
        base = input_file.resolve().parent
        data = yaml.safe_load(input_file.read_text())
        # Resolve domain ontology file paths relative to the input file's directory
        # so the YAML can use relative paths regardless of the working directory.
        try:
            for entry in data.get("bootstrap", {}).get("domain_ontologies", []):
                p = pathlib.Path(entry["file"])
                if not p.is_absolute():
                    entry["file"] = str((base / p).resolve())
        except (AttributeError, KeyError, TypeError):
            pass
        return SimulationInput.model_validate(data)

    def run(self, start_time: datetime | None = None) -> list[dict]:
        """Run the simulation and return a record for every event that fired.

        Each record contains:

        * ``"source"`` — always ``"simulation"``
        * ``"type"`` — the event type discriminator (e.g. ``"partition"``)
        * ``"time_offset_seconds"`` — the declared offset from simulation start
        * ``"fired_at"`` — ISO-8601 UTC timestamp of when the event was actually dispatched
        * ``"details"`` — the full event serialised as a plain dict
        * ``"_sort_time"`` — same as ``"fired_at"``; consumed by ``EventDumpService``
        """
        if start_time is None:
            start_time = datetime.now(timezone.utc)

        speed = self._simulation_input.speed
        sorted_events = sorted(
            list(self._simulation_input.events) + self._extra_events,
            key=lambda e: e.time_offset.to_seconds(),
        )

        logger.info(
            "Starting simulation '%s': %d event(s) at %.1fx speed.",
            self._simulation_input.name,
            len(sorted_events),
            speed,
        )

        fired: list[dict] = []

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

            fired_at = datetime.now(timezone.utc).isoformat()
            fired.append(
                {
                    "source": "simulation",
                    "type": event.type,
                    "time_offset_seconds": event.time_offset.to_seconds(),
                    "fired_at": fired_at,
                    "details": event.model_dump(mode="json"),
                    "_sort_time": fired_at,
                }
            )

        logger.info("Simulation '%s' complete.", self._simulation_input.name)

        simulation_responses = [r for r in fired if r["type"] == "simulation"]
        if simulation_responses:
            logger.info("Simulation event responses (%d):", len(simulation_responses))
            for record in simulation_responses:
                details = record["details"]
                logger.info(
                    "  [%s] %s -> %s: %s",
                    record["fired_at"],
                    details["target_agent"],
                    details["endpoint"],
                    details.get("response"),
                )

        return fired
