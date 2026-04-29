import logging
import time

import httpx

from util_simulation_service.model.agent import Agent
from util_simulation_service.model.graph_deployment_input import GraphDeploymentInput

logger = logging.getLogger(__name__)

_DEPLOY_ENDPOINT = "/ubiquia/core/flow-service/graph/deploy"
_MAX_ATTEMPTS = 24
_RETRY_INTERVAL_SECONDS = 10.0


class GraphDeploymentService:
    """Deploys registered graphs to their configured target agents.

    Graphs are embedded in — and registered alongside — a domain ontology.
    Deployment is a separate step that activates a registered graph on a
    flow-service instance, optionally applying node overrides via a flag.

    Transport-layer failures are retried up to _MAX_ATTEMPTS times to tolerate
    agents that are still starting up.  HTTP errors (4xx/5xx) are logged and
    not retried.
    """

    def __init__(self, agents: list[Agent]) -> None:
        self._agents = {a.name: a for a in agents}

    def deploy(self, agent_name: str, graph_deployments: list[GraphDeploymentInput]) -> None:
        """POST each graph deployment to the named agent's flow-service."""
        agent = self._agents.get(agent_name)
        if agent is None:
            logger.warning(
                "Graph deployment target '%s' not found in agents; skipping.", agent_name
            )
            return

        with httpx.Client() as client:
            for deployment in graph_deployments:
                logger.info(
                    "Deploying graph '%s' to agent '%s' (flag=%s).",
                    deployment.graph_name,
                    agent_name,
                    deployment.flag,
                )
                self._post_with_retry(client, agent, deployment)

    def _post_with_retry(
        self,
        client: httpx.Client,
        agent: Agent,
        deployment: GraphDeploymentInput,
    ) -> None:
        url = f"{agent.base_url}{_DEPLOY_ENDPOINT}"
        body = _build_body(deployment)

        for attempt in range(1, _MAX_ATTEMPTS + 1):
            try:
                response = client.post(url, json=body, timeout=10.0)
                response.raise_for_status()
                logger.info(
                    "Deployed graph '%s' to '%s'.", deployment.graph_name, agent.name
                )
                return
            except httpx.HTTPStatusError as exc:
                if exc.response.status_code < 500:
                    if "already deployed" in exc.response.text:
                        logger.warning(
                            "Graph '%s' is already deployed on '%s'; skipping.",
                            deployment.graph_name, agent.name,
                        )
                    else:
                        logger.error(
                            "Could not deploy graph '%s' to '%s': %s — response body: %s",
                            deployment.graph_name, agent.name, exc, exc.response.text,
                        )
                    return
                if attempt < _MAX_ATTEMPTS:
                    logger.warning(
                        "Agent '%s' returned %d (attempt %d/%d); retrying in %.0fs: %s",
                        agent.name, exc.response.status_code, attempt, _MAX_ATTEMPTS,
                        _RETRY_INTERVAL_SECONDS, exc,
                    )
                    time.sleep(_RETRY_INTERVAL_SECONDS)
                else:
                    logger.error(
                        "Could not deploy graph '%s' to '%s' after %d attempts: %s",
                        deployment.graph_name, agent.name, _MAX_ATTEMPTS, exc,
                    )
            except httpx.TransportError as exc:
                if attempt < _MAX_ATTEMPTS:
                    logger.warning(
                        "Agent '%s' not reachable (attempt %d/%d); retrying in %.0fs: %s",
                        agent.name, attempt, _MAX_ATTEMPTS, _RETRY_INTERVAL_SECONDS, exc,
                    )
                    time.sleep(_RETRY_INTERVAL_SECONDS)
                else:
                    logger.error(
                        "Could not reach agent '%s' after %d attempts: %s",
                        agent.name, _MAX_ATTEMPTS, exc,
                    )
            except Exception as exc:
                logger.error(
                    "Could not deploy graph '%s' to '%s': %s",
                    deployment.graph_name, agent.name, exc,
                )
                return


def _build_body(deployment: GraphDeploymentInput) -> dict:
    body: dict = {
        "graphName": deployment.graph_name,
        "domainOntologyName": deployment.domain_ontology_name,
        "domainVersion": {
            "major": deployment.domain_version.major,
            "minor": deployment.domain_version.minor,
            "patch": deployment.domain_version.patch,
        },
    }
    if deployment.flag is not None:
        body["graphSettings"] = {"flag": deployment.flag}
    return body
