from abc import ABC, abstractmethod

from util_simulation_service.model.agent import Agent


class AgentBuilder(ABC):
    """Abstract base for mode-specific Ubiquia agent builders."""

    @abstractmethod
    def build(
        self,
        agent_name: str,
        sync_enabled: bool = False,
        kubernetes_peer_urls: list[str] | None = None,
    ) -> Agent:
        """Provision a Ubiquia agent and return its connection details.

        Args:
            agent_name: Logical name and (where applicable) container/cluster name.
            sync_enabled: Whether cross-agent synchronization should be enabled.
                Applicable only to microweight agents; other builders may ignore it.
            kubernetes_peer_urls: Base URLs of Kubernetes peers in the same network.
                Applicable only to microweight agents; other builders may ignore it.
        """
