import logging
import time

import httpx
import yaml

from util_simulation_service.model.agent import Agent
from util_simulation_service.model.domain_ontology_bootstrap_input import DomainOntologyBootstrapInput

logger = logging.getLogger(__name__)

_DOMAIN_ONTOLOGY_ENDPOINT = "/ubiquia/core/flow-service/domain-ontology/register/post"
_MAX_ATTEMPTS = 24
_RETRY_INTERVAL_SECONDS = 10.0


class DomainOntologyBootstrapService:
    """POSTs domain ontology files to their configured target agents.

    Reads each ontology from the YAML file specified in the simulation input and
    sends it to the flow-service registration endpoint on every listed target agent.
    Connection failures are retried up to _MAX_ATTEMPTS times to tolerate agents
    that are still starting up. HTTP errors (4xx/5xx) are logged and not retried.
    """

    def __init__(self, agents: list[Agent]) -> None:
        self._agents = {a.name: a for a in agents}

    def bootstrap(self, domain_ontologies: list[DomainOntologyBootstrapInput]) -> None:
        """POST each domain ontology to its target agents."""
        with httpx.Client() as client:
            for entry in domain_ontologies:
                logger.info("Bootstrapping domain ontology from '%s'.", entry.file)
                payload = yaml.safe_load(entry.file.read_text(encoding="utf-8"))
                for target in entry.targets:
                    agent = self._agents.get(target)
                    if agent is None:
                        logger.warning(
                            "Bootstrap target '%s' not found in agents; skipping.", target
                        )
                        continue
                    self._post_with_retry(client, agent, payload)

    def _post_with_retry(self, client: httpx.Client, agent: Agent, payload: dict) -> None:
        url = f"{agent.base_url}{_DOMAIN_ONTOLOGY_ENDPOINT}"
        for attempt in range(1, _MAX_ATTEMPTS + 1):
            try:
                response = client.post(url, json=payload, timeout=10.0)
                response.raise_for_status()
                logger.info("Successfully posted domain ontology to '%s'.", agent.name)
                return
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
                logger.error("Could not post domain ontology to '%s': %s", agent.name, exc)
                return
