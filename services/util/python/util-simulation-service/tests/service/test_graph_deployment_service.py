from unittest.mock import MagicMock, patch

import httpx
import pytest

from util_simulation_service.model.agent import Agent
from util_simulation_service.model.graph_deployment_input import GraphDeploymentInput
from util_simulation_service.model.semantic_version import SemanticVersion
from util_simulation_service.service.logic.pre_processing.graph_deployment_service import (
    _DEPLOY_ENDPOINT,
    GraphDeploymentService,
)

_PATCH = "util_simulation_service.service.logic.pre_processing.graph_deployment_service.httpx.Client"

_AGENT = Agent(name="agent-a", base_url="http://localhost:8080")

_VERSION = SemanticVersion(major=1, minor=2, patch=3)


def _deployment(graph_name: str = "pet-store-dag", flag: str | None = None) -> GraphDeploymentInput:
    return GraphDeploymentInput(
        graph_name=graph_name,
        domain_ontology_name="pets",
        domain_version=_VERSION,
        flag=flag,
    )


def _mock_client(side_effect=None):
    mock_response = MagicMock(spec=httpx.Response)
    mock_response.raise_for_status.return_value = None
    mock_inner = MagicMock(spec=httpx.Client)
    mock_inner.post.return_value = mock_response
    if side_effect is not None:
        mock_inner.post.side_effect = side_effect
    return mock_inner


class TestGraphDeploymentServiceDeploy:
    def test_posts_to_deploy_endpoint(self):
        mock_inner = _mock_client()
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            GraphDeploymentService(agents=[_AGENT]).deploy("agent-a", [_deployment()])

        url = mock_inner.post.call_args.args[0]
        assert url == f"{_AGENT.base_url}{_DEPLOY_ENDPOINT}"

    def test_body_contains_graph_and_ontology_fields(self):
        mock_inner = _mock_client()
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            GraphDeploymentService(agents=[_AGENT]).deploy("agent-a", [_deployment()])

        body = mock_inner.post.call_args.kwargs["json"]
        assert body["graphName"] == "pet-store-dag"
        assert body["domainOntologyName"] == "pets"
        assert body["domainVersion"] == {"major": 1, "minor": 2, "patch": 3}

    def test_flag_included_in_graph_settings_when_provided(self):
        mock_inner = _mock_client()
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            GraphDeploymentService(agents=[_AGENT]).deploy("agent-a", [_deployment(flag="demo")])

        body = mock_inner.post.call_args.kwargs["json"]
        assert body["graphSettings"] == {"flag": "demo"}

    def test_graph_settings_omitted_when_no_flag(self):
        mock_inner = _mock_client()
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            GraphDeploymentService(agents=[_AGENT]).deploy("agent-a", [_deployment(flag=None)])

        body = mock_inner.post.call_args.kwargs["json"]
        assert "graphSettings" not in body

    def test_multiple_deployments_each_posted(self):
        mock_inner = _mock_client()
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            GraphDeploymentService(agents=[_AGENT]).deploy(
                "agent-a",
                [_deployment("graph-1"), _deployment("graph-2")],
            )

        assert mock_inner.post.call_count == 2

    def test_unknown_agent_skipped_without_error(self):
        mock_inner = _mock_client()
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            GraphDeploymentService(agents=[_AGENT]).deploy("unknown-agent", [_deployment()])

        mock_inner.post.assert_not_called()

    def test_transport_error_is_retried(self):
        success = MagicMock(spec=httpx.Response)
        success.raise_for_status.return_value = None
        mock_inner = _mock_client(
            side_effect=[httpx.RemoteProtocolError("disconnected"), success]
        )
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            with patch("util_simulation_service.service.logic.pre_processing.graph_deployment_service.time.sleep"):
                GraphDeploymentService(agents=[_AGENT]).deploy("agent-a", [_deployment()])

        assert mock_inner.post.call_count == 2

    def test_non_transport_error_not_retried(self):
        mock_inner = _mock_client(side_effect=ValueError("bad"))
        with patch(_PATCH) as MockClient:
            MockClient.return_value.__enter__.return_value = mock_inner
            GraphDeploymentService(agents=[_AGENT]).deploy("agent-a", [_deployment()])

        assert mock_inner.post.call_count == 1
