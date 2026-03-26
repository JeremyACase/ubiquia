class NetworkTopology:
    """Tracks which agents can reach which other agents based on shared network membership.

    An agent may belong to multiple networks. Its reachable peers are the union of all
    agents across every network it belongs to (excluding itself).
    """

    def __init__(self) -> None:
        self._peers: dict[str, set[str]] = {}

    def add_peer(self, agent_name: str, peer_name: str) -> None:
        self._peers.setdefault(agent_name, set()).add(peer_name)

    def peers_of(self, agent_name: str) -> frozenset[str]:
        """Returns all agents reachable from the given agent."""
        return frozenset(self._peers.get(agent_name, set()))

    def can_reach(self, source: str, target: str) -> bool:
        """Returns True if source and target share at least one network."""
        return target in self._peers.get(source, set())
