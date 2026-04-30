import pathlib
import subprocess
from unittest.mock import MagicMock, call, patch

import pytest

from util_simulation_service.service.builder.microweight_agent_builder import (
    MicroweightAgentBuilder,
    _CONTAINER_PORT,
    _IMAGE_NAME,
)
from util_simulation_service.model.agent import Agent


@pytest.fixture
def repo_root(tmp_path: pathlib.Path) -> pathlib.Path:
    """Minimal repo structure the builder references."""
    (tmp_path / "services" / "core" / "java" / "core-flow-service").mkdir(parents=True)
    (tmp_path / "deploy" / "compose" / "config").mkdir(parents=True)
    (tmp_path / "deploy" / "compose" / "config" / "application.yaml").touch()
    (tmp_path / "deploy" / "helm" / "bootstrap" / "ontologies").mkdir(parents=True)
    return tmp_path


def _make_run_side_effect(repo_root: pathlib.Path, host_port: str = "32768"):
    """Return a side_effect function for subprocess.run that answers inspection calls."""

    def _side_effect(cmd, **kwargs):
        result = MagicMock(spec=subprocess.CompletedProcess)
        result.returncode = 0

        if cmd[:3] == ["docker", "ps", "-a"]:
            result.stdout = ""  # no existing container
        elif cmd[:2] == ["docker", "port"]:
            result.stdout = f"0.0.0.0:{host_port}\n"
        else:
            result.stdout = ""

        return result

    return _side_effect


class TestMicroweightAgentBuilderBuild:
    def test_returns_agent_with_correct_name(self, repo_root):
        with patch("util_simulation_service.service.builder.microweight_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect(repo_root)):
            agent = MicroweightAgentBuilder(repo_root).build("agent-a")

        assert agent.name == "agent-a"

    def test_returns_agent_base_url_from_host_port(self, repo_root):
        with patch("util_simulation_service.service.builder.microweight_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect(repo_root, host_port="45678")):
            agent = MicroweightAgentBuilder(repo_root).build("agent-a")

        assert agent.base_url == "http://localhost:45678"

    def test_builds_docker_image(self, repo_root):
        with patch("util_simulation_service.service.builder.microweight_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect(repo_root)) as mock_run:
            MicroweightAgentBuilder(repo_root).build("agent-a")

        build_call = mock_run.call_args_list[0]
        cmd = build_call.args[0]
        assert cmd[:2] == ["docker", "build"]
        assert "--build-arg" in cmd
        assert "OPENJDK_VERSION=21" in cmd
        assert "-t" in cmd
        assert _IMAGE_NAME in cmd

    def test_runs_container_with_dynamic_port(self, repo_root):
        with patch("util_simulation_service.service.builder.microweight_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect(repo_root)) as mock_run:
            MicroweightAgentBuilder(repo_root).build("agent-a")

        run_call = next(c for c in mock_run.call_args_list if c.args[0][:2] == ["docker", "run"])
        cmd = run_call.args[0]
        assert "-d" in cmd
        assert "--name" in cmd and "agent-a" in cmd
        assert "-p" in cmd and _CONTAINER_PORT in cmd
        assert _IMAGE_NAME in cmd

    def test_runs_container_with_agent_id_env(self, repo_root):
        with patch("util_simulation_service.service.builder.microweight_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect(repo_root)) as mock_run:
            MicroweightAgentBuilder(repo_root).build("agent-a")

        run_call = next(c for c in mock_run.call_args_list if c.args[0][:2] == ["docker", "run"])
        cmd = run_call.args[0]
        assert "-e" in cmd
        env_vals = [cmd[i + 1] for i, v in enumerate(cmd) if v == "-e"]
        assert any(v.startswith("UBIQUIA_AGENT_ID=") for v in env_vals)

    def test_mounts_config_and_ontologies(self, repo_root):
        with patch("util_simulation_service.service.builder.microweight_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect(repo_root)) as mock_run:
            MicroweightAgentBuilder(repo_root).build("agent-a")

        run_call = next(c for c in mock_run.call_args_list if c.args[0][:2] == ["docker", "run"])
        cmd = " ".join(run_call.args[0])
        assert "application.yaml:/app/etc/application.yaml:ro" in cmd
        assert "ontologies:/app/etc/domain-ontologies:ro" in cmd

    def test_mounts_graphs_when_present(self, repo_root):
        graphs_dir = repo_root / "deploy" / "helm" / "bootstrap" / "graphs"
        graphs_dir.mkdir(parents=True)

        with patch("util_simulation_service.service.builder.microweight_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect(repo_root)) as mock_run:
            MicroweightAgentBuilder(repo_root).build("agent-a")

        run_call = next(c for c in mock_run.call_args_list if c.args[0][:2] == ["docker", "run"])
        cmd = " ".join(run_call.args[0])
        assert "graphs:/app/etc/graphs:ro" in cmd

    def test_does_not_mount_graphs_when_absent(self, repo_root):
        with patch("util_simulation_service.service.builder.microweight_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect(repo_root)) as mock_run:
            MicroweightAgentBuilder(repo_root).build("agent-a")

        run_call = next(c for c in mock_run.call_args_list if c.args[0][:2] == ["docker", "run"])
        cmd = " ".join(run_call.args[0])
        assert "/app/etc/graphs" not in cmd

    def test_removes_existing_container_before_starting(self, repo_root):
        def _side_effect_with_existing(cmd, **kwargs):
            result = MagicMock(spec=subprocess.CompletedProcess)
            result.returncode = 0
            if cmd[:3] == ["docker", "ps", "-a"]:
                result.stdout = "agent-a\n"
            elif cmd[:2] == ["docker", "port"]:
                result.stdout = "0.0.0.0:32768\n"
            else:
                result.stdout = ""
            return result

        with patch("util_simulation_service.service.builder.microweight_agent_builder.subprocess.run",
                   side_effect=_side_effect_with_existing) as mock_run:
            MicroweightAgentBuilder(repo_root).build("agent-a")

        rm_calls = [c for c in mock_run.call_args_list if c.args[0][:2] == ["docker", "rm"]]
        assert len(rm_calls) == 1
        assert "agent-a" in rm_calls[0].args[0]

    def test_parses_ipv6_port_format(self, repo_root):
        def _ipv6_side_effect(cmd, **kwargs):
            result = MagicMock(spec=subprocess.CompletedProcess)
            result.returncode = 0
            if cmd[:3] == ["docker", "ps", "-a"]:
                result.stdout = ""
            elif cmd[:2] == ["docker", "port"]:
                result.stdout = ":::54321\n"
            else:
                result.stdout = ""
            return result

        with patch("util_simulation_service.service.builder.microweight_agent_builder.subprocess.run",
                   side_effect=_ipv6_side_effect):
            agent = MicroweightAgentBuilder(repo_root).build("agent-a")

        assert agent.base_url == "http://localhost:54321"
