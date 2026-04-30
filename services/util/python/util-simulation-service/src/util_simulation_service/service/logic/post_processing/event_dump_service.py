import json
import logging
import pathlib

import httpx

from util_simulation_service.model.agent import Agent

logger = logging.getLogger(__name__)

_FLOW_EVENT_PATH = "/ubiquia/core/flow-service/flow-event/query/params"


def _strip_nulls(value: object) -> object:
    if isinstance(value, dict):
        return {k: _strip_nulls(v) for k, v in value.items() if v is not None}
    if isinstance(value, list):
        return [_strip_nulls(item) for item in value]
    return value


class EventDumpService:
    """Merges simulation-level events with agent flow events and writes a time-sorted JSON dump.

    Simulation events come from the caller as pre-built records that include the
    actual wall-clock time each event was dispatched.  Agent flow events are fetched
    by paging through the flow-service REST API on every agent.  All records are
    sorted by their representative timestamp before being written to disk.
    """

    def __init__(self, agents: list[Agent]) -> None:
        self._agents = agents

    def dump(
        self,
        simulation_name: str,
        fired_events: list[dict],
        output_dir: pathlib.Path = pathlib.Path("."),
        output_file_name: str | None = None,
    ) -> pathlib.Path:
        """Collect all events, sort by time, and write the event dump JSON file.

        Parameters
        ----------
        simulation_name:
            Used to derive the default output filename when ``output_file_name``
            is not provided.
        fired_events:
            Records produced by ``SimulationService.run()``.  Each must contain a
            ``"_sort_time"`` key with an ISO-8601 string used for ordering.
        output_dir:
            Directory in which to write the JSON file.  Defaults to the current
            working directory.
        output_file_name:
            Explicit filename for the output file.  When omitted the file is
            named ``{simulation_name}-event-dump.json``.
        """
        records: list[dict] = list(fired_events)

        with httpx.Client(timeout=10.0) as client:
            for agent in self._agents:
                records.extend(self._collect_flow_events(client, agent))

        records.sort(key=lambda r: r.get("_sort_time", ""))

        for r in records:
            r.pop("_sort_time", None)

        file_name = output_file_name or f"{simulation_name}-event-dump.json"
        output_path = output_dir / file_name
        output_path.write_text(json.dumps([_strip_nulls(r) for r in records], indent=2, default=str))
        logger.info(
            "Event dump written to '%s' (%d record(s)).", output_path, len(records)
        )
        return output_path

    def _collect_flow_events(self, client: httpx.Client, agent: Agent) -> list[dict]:
        records: list[dict] = []
        page = 0

        while True:
            try:
                response = client.get(
                    f"{agent.base_url}{_FLOW_EVENT_PATH}",
                    params={"page": page, "size": 100},
                )
                response.raise_for_status()
            except Exception as exc:
                logger.warning(
                    "Could not fetch flow events from '%s': %s", agent.name, exc
                )
                break

            body = response.json()
            for event in body.get("content", []):
                times = event.get("flowEventTimes") or {}
                sort_time = times.get("eventStartTime") or event.get("createdAt", "")
                records.append(
                    {
                        "source": "agent",
                        "agent_name": agent.name,
                        "event_time": sort_time,
                        "details": event,
                        "_sort_time": sort_time,
                    }
                )

            if body.get("last", True):
                break

            page += 1

        logger.debug(
            "Collected %d flow event(s) from '%s'.", len(records), agent.name
        )
        return records
