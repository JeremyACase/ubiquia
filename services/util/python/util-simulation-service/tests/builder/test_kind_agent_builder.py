import pathlib
import subprocess
from unittest.mock import MagicMock, patch

import pytest

from util_simulation_service.service.builder.kind_agent_builder import (
    KindAgentBuilder,
    _HELM_RELEASE,
    _IMAGE_NAME,
    _NAMESPACE,
    _NODE_PORT,
)


@pytest.fixture
def repo_root(tmp_path: pathlib.Path) -> pathlib.Path:
    """Minimal repo structure the builder references."""
    (tmp_path / "services" / "core" / "java" / "core-flow-service").mkdir(parents=True)
    (tmp_path / "deploy" / "helm" / "configurations" / "dev").mkdir(parents=True)
    (tmp_path / "deploy" / "helm" / "configurations" / "dev" / "featherweight-dev.yaml").touch()
    (tmp_path / "deploy" / "config" / "dev").mkdir(parents=True)
    (tmp_path / "deploy" / "config" / "dev" / "ubiquia_dev_service_account.yaml").touch()
    (tmp_path / "deploy" / "config" / "dev" / "ubiquia_dev_kind_pv.yaml").touch()
    return tmp_path


def _make_run_side_effect(cluster_exists: bool = False, helm_installed: bool = False):
    def _side_effect(cmd, **kwargs):
        result = MagicMock(spec=subprocess.CompletedProcess)
        result.returncode = 0
        result.stdout = ""

        if cmd[0] == "kind" and "get" in cmd and "clusters" in cmd:
            result.stdout = "ubiquia-agent-a\n" if cluster_exists else ""
        elif cmd[0] == "kubectl" and "get" in cmd and "namespace" in cmd:
            result.returncode = 1 if not cluster_exists else 0
        elif cmd[0] == "helm" and "--kube-context" in cmd and "status" in cmd:
            result.returncode = 0 if helm_installed else 1

        return result

    return _side_effect


class TestKindAgentBuilderBuild:
    def test_returns_agent_with_correct_name(self, repo_root):
        with patch("util_simulation_service.service.builder.kind_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect()):
            agent = KindAgentBuilder(repo_root).build("agent-a")

        assert agent.name == "agent-a"

    def test_returns_base_url_with_base_port(self, repo_root):
        with patch("util_simulation_service.service.builder.kind_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect()):
            agent = KindAgentBuilder(repo_root, base_port=9090).build("agent-a")

        assert agent.base_url == "http://localhost:9090"

    def test_each_build_increments_port(self, repo_root):
        with patch("util_simulation_service.service.builder.kind_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect()):
            builder = KindAgentBuilder(repo_root, base_port=8080)
            agent_a = builder.build("agent-a")
            agent_b = builder.build("agent-b")

        assert agent_a.base_url == "http://localhost:8080"
        assert agent_b.base_url == "http://localhost:8081"

    def test_creates_cluster_when_not_present(self, repo_root):
        with patch("util_simulation_service.service.builder.kind_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect(cluster_exists=False)) as mock_run:
            KindAgentBuilder(repo_root).build("agent-a")

        create_calls = [c for c in mock_run.call_args_list
                        if c.args[0][:2] == ["kind", "create"]]
        assert len(create_calls) == 1
        cmd = create_calls[0].args[0]
        assert "--name" in cmd and "ubiquia-agent-a" in cmd

    def test_skips_cluster_creation_when_already_present(self, repo_root):
        with patch("util_simulation_service.service.builder.kind_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect(cluster_exists=True)) as mock_run:
            KindAgentBuilder(repo_root).build("agent-a")

        create_calls = [c for c in mock_run.call_args_list
                        if c.args[0][:2] == ["kind", "create"]]
        assert len(create_calls) == 0

    def test_cluster_config_uses_correct_node_port_and_host_port(self, repo_root):
        written_configs = []

        def _side_effect(cmd, **kwargs):
            result = MagicMock(spec=subprocess.CompletedProcess)
            result.returncode = 0
            result.stdout = ""
            if cmd[:2] == ["kind", "create"]:
                config_path = next(cmd[i + 1] for i, v in enumerate(cmd) if v == "--config")
                written_configs.append(pathlib.Path(config_path).read_text())
            return result

        with patch("util_simulation_service.service.builder.kind_agent_builder.subprocess.run",
                   side_effect=_side_effect):
            KindAgentBuilder(repo_root, base_port=9000).build("agent-a")

        assert len(written_configs) == 1
        config = written_configs[0]
        assert f"containerPort: {_NODE_PORT}" in config
        assert "hostPort: 9000" in config

    def test_builds_and_loads_docker_image(self, repo_root):
        with patch("util_simulation_service.service.builder.kind_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect()) as mock_run:
            KindAgentBuilder(repo_root).build("agent-a")

        build_calls = [c for c in mock_run.call_args_list if c.args[0][:2] == ["docker", "build"]]
        load_calls = [c for c in mock_run.call_args_list if c.args[0][:2] == ["kind", "load"]]

        assert len(build_calls) == 1
        assert f"{_IMAGE_NAME}:latest" in build_calls[0].args[0]

        assert len(load_calls) == 1
        assert f"{_IMAGE_NAME}:latest" in load_calls[0].args[0]
        assert "ubiquia-agent-a" in load_calls[0].args[0]

    def test_applies_rbac_and_pv_manifests(self, repo_root):
        with patch("util_simulation_service.service.builder.kind_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect()) as mock_run:
            KindAgentBuilder(repo_root).build("agent-a")

        apply_calls = [c for c in mock_run.call_args_list if "apply" in c.args[0]]
        applied_files = [" ".join(c.args[0]) for c in apply_calls]

        assert any("ubiquia_dev_service_account.yaml" in f for f in applied_files)
        assert any("ubiquia_dev_kind_pv.yaml" in f for f in applied_files)

    def _helm_action_calls(self, mock_run) -> list:
        return [
            c for c in mock_run.call_args_list
            if c.args[0][0] == "helm"
            and "--kube-context" in c.args[0]
            and ("install" in c.args[0] or "upgrade" in c.args[0])
        ]

    def test_helm_install_on_fresh_cluster(self, repo_root):
        with patch("util_simulation_service.service.builder.kind_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect(helm_installed=False)) as mock_run:
            KindAgentBuilder(repo_root).build("agent-a")

        action_calls = self._helm_action_calls(mock_run)
        assert len(action_calls) == 1
        assert "install" in action_calls[0].args[0]
        assert "upgrade" not in action_calls[0].args[0]

    def test_helm_upgrade_on_existing_release(self, repo_root):
        with patch("util_simulation_service.service.builder.kind_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect(helm_installed=True)) as mock_run:
            KindAgentBuilder(repo_root).build("agent-a")

        action_calls = self._helm_action_calls(mock_run)
        assert len(action_calls) == 1
        assert "upgrade" in action_calls[0].args[0]
        assert "install" not in action_calls[0].args[0]

    def test_helm_uses_featherweight_values(self, repo_root):
        with patch("util_simulation_service.service.builder.kind_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect()) as mock_run:
            KindAgentBuilder(repo_root).build("agent-a")

        helm_calls = [c for c in mock_run.call_args_list if c.args[0][0] == "helm"
                      and ("install" in c.args[0] or "upgrade" in c.args[0])]
        assert len(helm_calls) == 1
        cmd = " ".join(helm_calls[0].args[0])
        assert "featherweight-dev.yaml" in cmd

    def test_kubectl_commands_target_correct_context(self, repo_root):
        with patch("util_simulation_service.service.builder.kind_agent_builder.subprocess.run",
                   side_effect=_make_run_side_effect()) as mock_run:
            KindAgentBuilder(repo_root).build("agent-a")

        kubectl_calls = [c for c in mock_run.call_args_list if c.args[0][0] == "kubectl"]
        for c in kubectl_calls:
            assert "--context" in c.args[0]
            ctx_idx = c.args[0].index("--context")
            assert c.args[0][ctx_idx + 1] == "kind-ubiquia-agent-a"
