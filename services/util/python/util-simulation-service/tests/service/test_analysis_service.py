from unittest.mock import MagicMock

import httpx
import pytest

from util_simulation_service.service.analysis_service import AnalysisService


def _mock_client(pages: list[dict]) -> httpx.Client:
    """Build a mock httpx.Client whose .get() returns successive page responses."""
    client = MagicMock(spec=httpx.Client)
    responses = [_make_response(page) for page in pages]
    client.get.side_effect = responses
    return client


def _make_response(body: dict) -> MagicMock:
    resp = MagicMock(spec=httpx.Response)
    resp.json.return_value = body
    resp.raise_for_status.return_value = None
    return resp


class TestAnalysisServiceCollectEvents:
    def test_single_page_returns_all_events(self):
        events = [{"id": 1}, {"id": 2}]
        client = _mock_client([{"content": events, "last": True}])
        service = AnalysisService()

        result = service._collect_events(client, "http://agent-a:8080")

        assert result == events

    def test_multiple_pages_concatenated(self):
        page1 = {"content": [{"id": 1}], "last": False}
        page2 = {"content": [{"id": 2}], "last": True}
        client = _mock_client([page1, page2])
        service = AnalysisService()

        result = service._collect_events(client, "http://agent-a:8080")

        assert result == [{"id": 1}, {"id": 2}]

    def test_pagination_increments_page_param(self):
        page1 = {"content": [{"id": 1}], "last": False}
        page2 = {"content": [{"id": 2}], "last": True}
        client = _mock_client([page1, page2])
        service = AnalysisService()

        service._collect_events(client, "http://agent-a:8080")

        calls = client.get.call_args_list
        assert calls[0].kwargs["params"]["page"] == 0
        assert calls[1].kwargs["params"]["page"] == 1

    def test_page_size_is_100(self):
        client = _mock_client([{"content": [], "last": True}])
        service = AnalysisService()

        service._collect_events(client, "http://agent-a:8080")

        client.get.assert_called_once_with(
            "http://agent-a:8080/flow-events",
            params={"page": 0, "size": 100},
        )

    def test_empty_content_returns_empty_list(self):
        client = _mock_client([{"content": [], "last": True}])
        service = AnalysisService()

        result = service._collect_events(client, "http://agent-a:8080")

        assert result == []

    def test_missing_last_key_defaults_to_stop(self):
        # body.get("last", True) means missing "last" stops pagination
        client = _mock_client([{"content": [{"id": 1}]}])
        service = AnalysisService()

        result = service._collect_events(client, "http://agent-a:8080")

        assert result == [{"id": 1}]
        assert client.get.call_count == 1

    def test_http_error_propagates(self):
        client = MagicMock(spec=httpx.Client)
        resp = MagicMock(spec=httpx.Response)
        resp.raise_for_status.side_effect = httpx.HTTPStatusError(
            "404", request=MagicMock(), response=MagicMock()
        )
        client.get.return_value = resp
        service = AnalysisService()

        with pytest.raises(httpx.HTTPStatusError):
            service._collect_events(client, "http://agent-a:8080")
