import pathlib
from unittest.mock import MagicMock, patch

import httpx
import pytest

from util_simulation_service.model.agent import Agent
from util_simulation_service.model.domain_ontology_bootstrap_input import DomainOntologyBootstrapInput
from util_simulation_service.service.domain_ontology_bootstrap_service import (
    _DOMAIN_ONTOLOGY_ENDPOINT,
    _MAX_ATTEMPTS,
    DomainOntologyBootstrapService,
)

_AGENT_A = Agent(name="agent-a", base_url="http://localhost:8080")
_AGENT_B = Agent(name="agent-b", base_url="http://localhost:8081")

_PATCH = "util_simulation_service.service.domain_ontology_bootstrap_service.httpx.Client"

_ONTOLOGY_PAYLOAD = {"name": "pets", "version": {"major": 1, "minor": 0, "patch": 0}}


def _mock_client(side_effect=None):
    mock_response = MagicMock(spec=httpx.Response)
    mock_inner = MagicMock(spec=httpx.Client)
    mock_inner.post.return_value = mock_response
    if side_effect is not None:
        mock_inner.post.side_effect = side_effect
    return mock_inner


def _entry(file: pathlib.Path, targets: list[str]) -> DomainOntologyBootstrapInput:
    return DomainOntologyBootstrapInput(file=file, targets=targets)


class TestDomainOntologyBootstrapService:
    def test_posts_ontology_to_target_agent(self, tmp_path):
        ontology_file = tmp_path / "ontology.yaml"
        ontology_file.write_text("name: pets\n")
        mock_inner = _mock_client()

        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            DomainOntologyBootstrapService(agents=[_AGENT_A]).bootstrap(
                [_entry(ontology_file, ["agent-a"])]
            )

        mock_inner.post.assert_called_once()
        url = mock_inner.post.call_args.args[0]
        assert url == f"{_AGENT_A.base_url}{_DOMAIN_ONTOLOGY_ENDPOINT}"

    def test_posts_to_multiple_targets(self, tmp_path):
        ontology_file = tmp_path / "ontology.yaml"
        ontology_file.write_text("name: pets\n")
        mock_inner = _mock_client()

        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            DomainOntologyBootstrapService(agents=[_AGENT_A, _AGENT_B]).bootstrap(
                [_entry(ontology_file, ["agent-a", "agent-b"])]
            )

        assert mock_inner.post.call_count == 2
        posted_urls = [c.args[0] for c in mock_inner.post.call_args_list]
        assert f"{_AGENT_A.base_url}{_DOMAIN_ONTOLOGY_ENDPOINT}" in posted_urls
        assert f"{_AGENT_B.base_url}{_DOMAIN_ONTOLOGY_ENDPOINT}" in posted_urls

    def test_ontology_is_sent_as_json(self, tmp_path):
        ontology_file = tmp_path / "ontology.yaml"
        ontology_file.write_text("name: pets\nversion:\n  major: 1\n  minor: 0\n  patch: 0\n")
        mock_inner = _mock_client()

        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            DomainOntologyBootstrapService(agents=[_AGENT_A]).bootstrap(
                [_entry(ontology_file, ["agent-a"])]
            )

        sent_payload = mock_inner.post.call_args.kwargs["json"]
        assert sent_payload["name"] == "pets"
        assert sent_payload["version"]["major"] == 1

    def test_unknown_target_is_skipped_without_error(self, tmp_path):
        ontology_file = tmp_path / "ontology.yaml"
        ontology_file.write_text("name: pets\n")
        mock_inner = _mock_client()

        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            DomainOntologyBootstrapService(agents=[_AGENT_A]).bootstrap(
                [_entry(ontology_file, ["agent-unknown"])]
            )

        mock_inner.post.assert_not_called()

    def test_http_failure_does_not_stop_remaining_targets(self, tmp_path):
        ontology_file = tmp_path / "ontology.yaml"
        ontology_file.write_text("name: pets\n")
        mock_inner = _mock_client(side_effect=[Exception("refused"), MagicMock(spec=httpx.Response)])

        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            DomainOntologyBootstrapService(agents=[_AGENT_A, _AGENT_B]).bootstrap(
                [_entry(ontology_file, ["agent-a", "agent-b"])]
            )

        assert mock_inner.post.call_count == 2

    def test_multiple_ontology_entries_are_all_posted(self, tmp_path):
        file_a = tmp_path / "a.yaml"
        file_b = tmp_path / "b.yaml"
        file_a.write_text("name: ontology-a\n")
        file_b.write_text("name: ontology-b\n")
        mock_inner = _mock_client()

        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            DomainOntologyBootstrapService(agents=[_AGENT_A]).bootstrap(
                [
                    _entry(file_a, ["agent-a"]),
                    _entry(file_b, ["agent-a"]),
                ]
            )

        assert mock_inner.post.call_count == 2

    def test_no_entries_makes_no_http_calls(self, tmp_path):
        mock_inner = _mock_client()

        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            DomainOntologyBootstrapService(agents=[_AGENT_A]).bootstrap([])

        mock_inner.post.assert_not_called()

    def test_transport_error_is_retried(self, tmp_path):
        ontology_file = tmp_path / "ontology.yaml"
        ontology_file.write_text("name: pets\n")
        success = MagicMock(spec=httpx.Response)
        success.raise_for_status.return_value = None
        mock_inner = _mock_client(
            side_effect=[httpx.RemoteProtocolError("disconnected"), success]
        )

        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            with patch("util_simulation_service.service.domain_ontology_bootstrap_service.time.sleep"):
                DomainOntologyBootstrapService(agents=[_AGENT_A]).bootstrap(
                    [_entry(ontology_file, ["agent-a"])]
                )

        assert mock_inner.post.call_count == 2

    def test_non_transport_error_is_not_retried(self, tmp_path):
        ontology_file = tmp_path / "ontology.yaml"
        ontology_file.write_text("name: pets\n")
        mock_inner = _mock_client(side_effect=ValueError("bad payload"))

        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            DomainOntologyBootstrapService(agents=[_AGENT_A]).bootstrap(
                [_entry(ontology_file, ["agent-a"])]
            )

        assert mock_inner.post.call_count == 1
