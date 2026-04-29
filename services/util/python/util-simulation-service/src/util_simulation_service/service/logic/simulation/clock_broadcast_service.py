import logging
from datetime import datetime

import httpx

from util_simulation_service.model.agent import Agent
from util_simulation_service.model.agent_mode import AgentMode

logger = logging.getLogger(__name__)

_CLOCK_ENDPOINTS = [
    "/ubiquia/core/flow-service/simulation/clock/set",
    "/ubiquia/core/belief-state-generator-service/simulation/clock/set",
    "/ubiquia/core/communication-service/simulation/clock/set",
]

_MICROWEIGHT_CLOCK_ENDPOINTS = [
    "/ubiquia/core/flow-service/simulation/clock/set",
]


class ClockBroadcastService:
    """Broadcasts a simulated time to the SimulationController on every core service
    of every known Ubiquia agent.

    Microweight agents only run core-flow-service, so only that endpoint is
    targeted for them. KIND and TEST agents receive the full set of endpoints.
    """

    def __init__(self, agents: list[Agent]) -> None:
        self._agents = agents

    def broadcast(self, simulated_time: datetime) -> None:
        """POST the given simulated time to all core service clock endpoints on every agent."""
        logger.info(
            "Broadcasting simulated time %s to %d agent(s).",
            simulated_time.isoformat(),
            len(self._agents),
        )
        with httpx.Client() as client:
            for agent in self._agents:
                endpoints = (
                    _MICROWEIGHT_CLOCK_ENDPOINTS
                    if agent.mode == AgentMode.MICROWEIGHT
                    else _CLOCK_ENDPOINTS
                )
                for path in endpoints:
                    self._try_post(client, agent, path, simulated_time)

    def _try_post(
        self,
        client: httpx.Client,
        agent: Agent,
        path: str,
        simulated_time: datetime,
    ) -> None:
        url = f"{agent.base_url}{path}"
        try:
            response = client.post(url, json=simulated_time.isoformat())
            response.raise_for_status()
            logger.debug("Updated clock on %s (%s).", agent.name, path)
        except Exception as exc:
            logger.warning("Could not update clock on %s (%s): %s", agent.name, path, exc)
