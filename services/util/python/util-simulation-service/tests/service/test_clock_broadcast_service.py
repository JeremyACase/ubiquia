from datetime import datetime, timezone
from unittest.mock import MagicMock, patch

import httpx
import pytest

from util_simulation_service.model.agent import Agent
from util_simulation_service.model.agent_mode import AgentMode
from util_simulation_service.service.logic.simulation.clock_broadcast_service import (
    _CLOCK_ENDPOINTS,
    _MICROWEIGHT_CLOCK_ENDPOINTS,
    ClockBroadcastService,
)

_TIME = datetime(2025, 6, 15, 12, 0, 0, tzinfo=timezone.utc)
_AGENT_A = Agent(name="agent-a", base_url="http://localhost:8080")
_AGENT_B = Agent(name="agent-b", base_url="http://localhost:8081")
_MICROWEIGHT_AGENT = Agent(name="mw-agent", base_url="http://localhost:9090", mode=AgentMode.MICROWEIGHT)

_PATCH = "util_simulation_service.service.logic.simulation.clock_broadcast_service.httpx.Client"


def _mock_client(side_effects=None):
    """Patch httpx.Client and return the mock inner client used inside the `with` block."""
    mock_response = MagicMock(spec=httpx.Response)
    mock_inner = MagicMock(spec=httpx.Client)
    mock_inner.post.return_value = mock_response
    if side_effects is not None:
        mock_inner.post.side_effect = side_effects
    return mock_inner


class TestClockBroadcastServiceBroadcast:
    def test_posts_to_every_clock_endpoint_for_one_agent(self):
        mock_inner = _mock_client()
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            ClockBroadcastService(agents=[_AGENT_A]).broadcast(_TIME)

        assert mock_inner.post.call_count == len(_CLOCK_ENDPOINTS)

    def test_posts_to_every_endpoint_for_every_agent(self):
        mock_inner = _mock_client()
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            ClockBroadcastService(agents=[_AGENT_A, _AGENT_B]).broadcast(_TIME)

        assert mock_inner.post.call_count == len(_CLOCK_ENDPOINTS) * 2

    def test_urls_are_built_from_agent_base_url(self):
        mock_inner = _mock_client()
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            ClockBroadcastService(agents=[_AGENT_A]).broadcast(_TIME)

        posted_urls = [c.args[0] for c in mock_inner.post.call_args_list]
        for endpoint in _CLOCK_ENDPOINTS:
            assert f"{_AGENT_A.base_url}{endpoint}" in posted_urls

    def test_time_is_sent_as_iso8601_json_string(self):
        mock_inner = _mock_client()
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            ClockBroadcastService(agents=[_AGENT_A]).broadcast(_TIME)

        for call_obj in mock_inner.post.call_args_list:
            assert call_obj.kwargs["json"] == _TIME.isoformat()

    def test_endpoint_failure_does_not_stop_remaining_posts(self):
        side_effects = [Exception("refused")] + [MagicMock(spec=httpx.Response)] * (len(_CLOCK_ENDPOINTS) - 1)
        mock_inner = _mock_client(side_effects=side_effects)
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            # must not raise
            ClockBroadcastService(agents=[_AGENT_A]).broadcast(_TIME)

        assert mock_inner.post.call_count == len(_CLOCK_ENDPOINTS)

    def test_all_endpoints_failing_does_not_raise(self):
        mock_inner = _mock_client(side_effects=Exception("network down"))
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            ClockBroadcastService(agents=[_AGENT_A]).broadcast(_TIME)

    def test_no_agents_makes_no_http_calls(self):
        mock_inner = _mock_client()
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            ClockBroadcastService(agents=[]).broadcast(_TIME)

        mock_inner.post.assert_not_called()

    def test_second_agent_uses_its_own_base_url(self):
        mock_inner = _mock_client()
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            ClockBroadcastService(agents=[_AGENT_A, _AGENT_B]).broadcast(_TIME)

        posted_urls = [c.args[0] for c in mock_inner.post.call_args_list]
        assert any(_AGENT_B.base_url in url for url in posted_urls)


class TestClockBroadcastServiceMicroweight:
    def test_microweight_agent_only_posts_to_flow_service(self):
        mock_inner = _mock_client()
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            ClockBroadcastService(agents=[_MICROWEIGHT_AGENT]).broadcast(_TIME)

        assert mock_inner.post.call_count == len(_MICROWEIGHT_CLOCK_ENDPOINTS)
        posted_urls = [c.args[0] for c in mock_inner.post.call_args_list]
        assert all("/ubiquia/core/flow-service/" in url for url in posted_urls)

    def test_microweight_agent_does_not_post_to_comm_or_belief_state(self):
        mock_inner = _mock_client()
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            ClockBroadcastService(agents=[_MICROWEIGHT_AGENT]).broadcast(_TIME)

        posted_urls = [c.args[0] for c in mock_inner.post.call_args_list]
        assert not any("communication-service" in url for url in posted_urls)
        assert not any("belief-state-generator-service" in url for url in posted_urls)

    def test_mixed_agents_use_correct_endpoint_sets(self):
        mock_inner = _mock_client()
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            ClockBroadcastService(agents=[_AGENT_A, _MICROWEIGHT_AGENT]).broadcast(_TIME)

        expected_calls = len(_CLOCK_ENDPOINTS) + len(_MICROWEIGHT_CLOCK_ENDPOINTS)
        assert mock_inner.post.call_count == expected_calls
