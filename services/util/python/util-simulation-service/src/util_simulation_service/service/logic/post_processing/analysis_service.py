import httpx


class AnalysisService:
    """Queries all flow events from every flow service across all known Ubiquia agents.

    Pages through each agent's flow-service REST API until all events have
    been retrieved.
    """

    def __init__(self):
        pass

    def run(self) -> list[dict]:
        return []

    def _collect_events(self, client: httpx.Client, base_url: str) -> list[dict]:
        events = []
        page = 0

        while True:
            response = client.get(
                f"{base_url}/flow-events",
                params={"page": page, "size": 100},
            )
            response.raise_for_status()
            body = response.json()

            events.extend(body.get("content", []))

            if body.get("last", True):
                break

            page += 1

        return events
