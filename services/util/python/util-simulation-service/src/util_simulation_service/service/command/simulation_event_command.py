import logging

import httpx

from util_simulation_service.service.command.event_command import EventCommand
from util_simulation_service.model.agent import Agent
from util_simulation_service.model.events.event import Event
from util_simulation_service.model.events.simulation_event import SimulationEvent
from util_simulation_service.model.network_topology import NetworkTopology

logger = logging.getLogger(__name__)


class SimulationEventCommand(EventCommand):
    """Processes a SimulationEvent by POSTing its payload to the target agent's endpoint."""

    def __init__(self, agents: list[Agent] | None = None, topology: NetworkTopology | None = None) -> None:
        self._agents = {a.name: a for a in agents} if agents else {}
        self._topology = topology

    def execute(self, event: Event) -> None:
        if not isinstance(event, SimulationEvent):
            raise TypeError(f"Expected SimulationEvent, got {type(event).__name__}")

        agent = self._agents.get(event.target_agent)
        if agent is None:
            logger.warning(
                "SimulationEvent target '%s' not found in agents; skipping.", event.target_agent
            )
            return

        url = f"{agent.base_url}{event.endpoint}"
        logger.info("POSTing simulation event payload to '%s' at %s.", event.target_agent, url)
        try:
            http_response = httpx.post(url, json=event.payload, timeout=10.0)
            event.response = {"status_code": http_response.status_code, "body": http_response.text}
            http_response.raise_for_status()
            logger.info("Simulation event payload accepted by '%s'.", event.target_agent)
        except httpx.HTTPStatusError as exc:
            logger.error("Could not deliver simulation event to '%s': %s", event.target_agent, exc)
        except Exception as exc:
            event.response = {"error": str(exc)}
            logger.error("Could not deliver simulation event to '%s': %s", event.target_agent, exc)
