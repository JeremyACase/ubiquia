import json
import pathlib
from unittest.mock import MagicMock

import httpx
import pytest

from util_simulation_service.model.agent import Agent
from util_simulation_service.service.event_dump_service import EventDumpService


def _agent(name: str = "agent-a", base_url: str = "http://agent-a:8080") -> Agent:
    return Agent(name=name, base_url=base_url)


def _make_response(body: dict, status_code: int = 200) -> MagicMock:
    resp = MagicMock(spec=httpx.Response)
    resp.json.return_value = body
    resp.raise_for_status.return_value = None
    resp.status_code = status_code
    return resp


def _flow_event(event_id: str, event_start: str) -> dict:
    return {
        "id": event_id,
        "createdAt": event_start,
        "flowEventTimes": {"eventStartTime": event_start},
    }


class TestEventDumpServiceDump:
    def test_writes_json_file_to_output_dir(self, tmp_path):
        service = EventDumpService(agents=[])
        service.dump(simulation_name="my-sim", fired_events=[], output_dir=tmp_path)

        output = tmp_path / "my-sim-event-dump.json"
        assert output.exists()

    def test_output_is_valid_json(self, tmp_path):
        service = EventDumpService(agents=[])
        service.dump(simulation_name="my-sim", fired_events=[], output_dir=tmp_path)

        data = json.loads((tmp_path / "my-sim-event-dump.json").read_text())
        assert isinstance(data, list)

    def test_simulation_events_included_in_output(self, tmp_path):
        fired = [
            {
                "source": "simulation",
                "type": "partition",
                "time_offset_seconds": 30.0,
                "fired_at": "2026-04-10T12:00:30Z",
                "details": {"type": "partition"},
                "_sort_time": "2026-04-10T12:00:30Z",
            }
        ]
        service = EventDumpService(agents=[])
        service.dump(simulation_name="sim", fired_events=fired, output_dir=tmp_path)

        data = json.loads((tmp_path / "sim-event-dump.json").read_text())
        assert len(data) == 1
        assert data[0]["source"] == "simulation"
        assert data[0]["type"] == "partition"

    def test_sort_time_key_stripped_from_output(self, tmp_path):
        fired = [
            {
                "source": "simulation",
                "type": "partition",
                "fired_at": "2026-04-10T12:00:30Z",
                "_sort_time": "2026-04-10T12:00:30Z",
                "details": {},
            }
        ]
        service = EventDumpService(agents=[])
        service.dump(simulation_name="sim", fired_events=fired, output_dir=tmp_path)

        data = json.loads((tmp_path / "sim-event-dump.json").read_text())
        assert "_sort_time" not in data[0]

    def test_returns_output_path(self, tmp_path):
        service = EventDumpService(agents=[])
        result = service.dump(simulation_name="my-sim", fired_events=[], output_dir=tmp_path)

        assert result == tmp_path / "my-sim-event-dump.json"

    def test_default_output_dir_is_cwd(self, tmp_path, monkeypatch):
        monkeypatch.chdir(tmp_path)
        service = EventDumpService(agents=[])
        result = service.dump(simulation_name="my-sim", fired_events=[])

        assert result == pathlib.Path(".") / "my-sim-event-dump.json"
        assert (tmp_path / "my-sim-event-dump.json").exists()

    def test_output_file_name_overrides_default(self, tmp_path):
        service = EventDumpService(agents=[])
        result = service.dump(
            simulation_name="my-sim",
            fired_events=[],
            output_dir=tmp_path,
            output_file_name="custom-dump.json",
        )

        assert result == tmp_path / "custom-dump.json"
        assert (tmp_path / "custom-dump.json").exists()
        assert not (tmp_path / "my-sim-event-dump.json").exists()

    def test_output_file_name_none_uses_simulation_name(self, tmp_path):
        service = EventDumpService(agents=[])
        result = service.dump(
            simulation_name="my-sim",
            fired_events=[],
            output_dir=tmp_path,
            output_file_name=None,
        )

        assert result == tmp_path / "my-sim-event-dump.json"


class TestEventDumpServiceCollectFlowEvents:
    def _service_with_client(self, client: httpx.Client, agents: list[Agent]) -> EventDumpService:
        service = EventDumpService(agents=agents)
        return service

    def test_single_page_collected(self, tmp_path):
        agent = _agent()
        flow_events = [_flow_event("id-1", "2026-04-10T12:00:01Z")]
        client = MagicMock(spec=httpx.Client)
        client.get.return_value = _make_response({"content": flow_events, "last": True})

        service = EventDumpService(agents=[agent])
        result = service._collect_flow_events(client, agent)

        assert len(result) == 1
        assert result[0]["source"] == "agent"
        assert result[0]["agent_name"] == "agent-a"
        assert result[0]["details"]["id"] == "id-1"

    def test_multiple_pages_concatenated(self, tmp_path):
        agent = _agent()
        page1 = {"content": [_flow_event("id-1", "2026-04-10T12:00:01Z")], "last": False}
        page2 = {"content": [_flow_event("id-2", "2026-04-10T12:00:02Z")], "last": True}
        client = MagicMock(spec=httpx.Client)
        client.get.side_effect = [_make_response(page1), _make_response(page2)]

        service = EventDumpService(agents=[agent])
        result = service._collect_flow_events(client, agent)

        assert len(result) == 2
        assert result[0]["details"]["id"] == "id-1"
        assert result[1]["details"]["id"] == "id-2"

    def test_pagination_increments_page_param(self):
        agent = _agent()
        page1 = {"content": [_flow_event("id-1", "2026-04-10T12:00:01Z")], "last": False}
        page2 = {"content": [], "last": True}
        client = MagicMock(spec=httpx.Client)
        client.get.side_effect = [_make_response(page1), _make_response(page2)]

        service = EventDumpService(agents=[agent])
        service._collect_flow_events(client, agent)

        assert client.get.call_args_list[0].kwargs["params"]["page"] == 0
        assert client.get.call_args_list[1].kwargs["params"]["page"] == 1

    def test_uses_correct_endpoint(self):
        agent = _agent(base_url="http://my-agent:8080")
        client = MagicMock(spec=httpx.Client)
        client.get.return_value = _make_response({"content": [], "last": True})

        service = EventDumpService(agents=[agent])
        service._collect_flow_events(client, agent)

        url = client.get.call_args[0][0]
        assert url == "http://my-agent:8080/ubiquia/core/flow-service/flow-event/query/params"

    def test_sort_time_uses_event_start_time(self):
        agent = _agent()
        event_start = "2026-04-10T12:00:05Z"
        client = MagicMock(spec=httpx.Client)
        client.get.return_value = _make_response(
            {"content": [_flow_event("id-1", event_start)], "last": True}
        )

        service = EventDumpService(agents=[agent])
        result = service._collect_flow_events(client, agent)

        assert result[0]["_sort_time"] == event_start
        assert result[0]["event_time"] == event_start

    def test_sort_time_falls_back_to_created_at(self):
        agent = _agent()
        event = {"id": "id-1", "createdAt": "2026-04-10T12:00:05Z", "flowEventTimes": None}
        client = MagicMock(spec=httpx.Client)
        client.get.return_value = _make_response({"content": [event], "last": True})

        service = EventDumpService(agents=[agent])
        result = service._collect_flow_events(client, agent)

        assert result[0]["_sort_time"] == "2026-04-10T12:00:05Z"

    def test_network_error_logged_and_returns_partial(self):
        agent = _agent()
        client = MagicMock(spec=httpx.Client)
        client.get.side_effect = httpx.ConnectError("refused")

        service = EventDumpService(agents=[agent])
        result = service._collect_flow_events(client, agent)

        assert result == []

    def test_http_error_logged_and_returns_partial(self):
        agent = _agent()
        client = MagicMock(spec=httpx.Client)
        resp = MagicMock(spec=httpx.Response)
        resp.raise_for_status.side_effect = httpx.HTTPStatusError(
            "500", request=MagicMock(), response=MagicMock()
        )
        client.get.return_value = resp

        service = EventDumpService(agents=[agent])
        result = service._collect_flow_events(client, agent)

        assert result == []


class TestEventDumpServiceSorting:
    def test_events_sorted_by_time_ascending(self, tmp_path):
        fired = [
            {
                "source": "simulation",
                "type": "partition",
                "fired_at": "2026-04-10T12:00:30Z",
                "_sort_time": "2026-04-10T12:00:30Z",
                "details": {},
            },
            {
                "source": "simulation",
                "type": "agent_join",
                "fired_at": "2026-04-10T12:00:10Z",
                "_sort_time": "2026-04-10T12:00:10Z",
                "details": {},
            },
        ]
        service = EventDumpService(agents=[])
        service.dump(simulation_name="sim", fired_events=fired, output_dir=tmp_path)

        data = json.loads((tmp_path / "sim-event-dump.json").read_text())
        assert data[0]["fired_at"] == "2026-04-10T12:00:10Z"
        assert data[1]["fired_at"] == "2026-04-10T12:00:30Z"

    def test_agent_events_interleaved_with_simulation_events(self, tmp_path):
        agent = _agent()
        fired = [
            {
                "source": "simulation",
                "type": "partition",
                "fired_at": "2026-04-10T12:00:30Z",
                "_sort_time": "2026-04-10T12:00:30Z",
                "details": {},
            }
        ]
        flow_event = _flow_event("id-1", "2026-04-10T12:00:15Z")
        client = MagicMock(spec=httpx.Client)
        client.__enter__ = lambda s: client
        client.__exit__ = MagicMock(return_value=False)
        client.get.return_value = _make_response({"content": [flow_event], "last": True})

        service = EventDumpService(agents=[agent])

        # Patch httpx.Client context manager to inject our mock
        import unittest.mock as mock
        with mock.patch("util_simulation_service.service.event_dump_service.httpx.Client") as MockClient:
            MockClient.return_value.__enter__ = lambda s: client
            MockClient.return_value.__exit__ = MagicMock(return_value=False)
            client.get.return_value = _make_response({"content": [flow_event], "last": True})
            service.dump(simulation_name="sim", fired_events=fired, output_dir=tmp_path)

        data = json.loads((tmp_path / "sim-event-dump.json").read_text())
        assert len(data) == 2
        # Agent event at 12:00:15 comes before simulation event at 12:00:30
        assert data[0]["source"] == "agent"
        assert data[1]["source"] == "simulation"

    def test_multiple_agents_all_collected(self, tmp_path):
        agents = [_agent("agent-a", "http://a:8080"), _agent("agent-b", "http://b:8080")]
        event_a = _flow_event("id-a", "2026-04-10T12:00:01Z")
        event_b = _flow_event("id-b", "2026-04-10T12:00:02Z")

        import unittest.mock as mock
        with mock.patch("util_simulation_service.service.event_dump_service.httpx.Client") as MockClient:
            client = MagicMock()
            MockClient.return_value.__enter__ = lambda s: client
            MockClient.return_value.__exit__ = MagicMock(return_value=False)
            client.get.side_effect = [
                _make_response({"content": [event_a], "last": True}),
                _make_response({"content": [event_b], "last": True}),
            ]
            service = EventDumpService(agents=agents)
            service.dump(simulation_name="sim", fired_events=[], output_dir=tmp_path)

        data = json.loads((tmp_path / "sim-event-dump.json").read_text())
        assert len(data) == 2
        names = {r["agent_name"] for r in data}
        assert names == {"agent-a", "agent-b"}
