import subprocess
from unittest.mock import MagicMock, patch

import pytest

from util_simulation_service.builder.docker_network_builder import DockerNetworkBuilder
from util_simulation_service.model.agent import Agent
from util_simulation_service.model.network import Network


def _agent(name: str) -> Agent:
    return Agent(name=name, base_url=f"http://localhost:8080")


def _network(name: str, agents: list[str]) -> Network:
    return Network(name=name, agents=agents)


def _make_run_side_effect(network_exists: bool = False):
    def _side_effect(cmd, **kwargs):
        result = MagicMock(spec=subprocess.CompletedProcess)
        result.returncode = 0
        result.stdout = ""
        result.stderr = ""

        if cmd[:3] == ["docker", "network", "ls"]:
            result.stdout = "my-network\n" if network_exists else ""
        return result

    return _side_effect


class TestDockerNetworkBuilderBuild:
    def test_creates_network_when_absent(self):
        with patch("util_simulation_service.builder.docker_network_builder.subprocess.run",
                   side_effect=_make_run_side_effect(network_exists=False)) as mock_run:
            DockerNetworkBuilder().build(_network("my-network", ["a"]), [_agent("a")])

        create_calls = [c for c in mock_run.call_args_list
                        if c.args[0][:3] == ["docker", "network", "create"]]
        assert len(create_calls) == 1
        assert "my-network" in create_calls[0].args[0]

    def test_skips_network_creation_when_already_exists(self):
        with patch("util_simulation_service.builder.docker_network_builder.subprocess.run",
                   side_effect=_make_run_side_effect(network_exists=True)) as mock_run:
            DockerNetworkBuilder().build(_network("my-network", ["a"]), [_agent("a")])

        create_calls = [c for c in mock_run.call_args_list
                        if c.args[0][:3] == ["docker", "network", "create"]]
        assert len(create_calls) == 0

    def test_connects_each_agent_to_network(self):
        with patch("util_simulation_service.builder.docker_network_builder.subprocess.run",
                   side_effect=_make_run_side_effect()) as mock_run:
            DockerNetworkBuilder().build(
                _network("my-network", ["agent-a", "agent-b"]),
                [_agent("agent-a"), _agent("agent-b")],
            )

        connect_calls = [c for c in mock_run.call_args_list
                         if c.args[0][:3] == ["docker", "network", "connect"]]
        connected = [c.args[0][4] for c in connect_calls]
        assert "agent-a" in connected
        assert "agent-b" in connected

    def test_connect_uses_correct_network_name(self):
        with patch("util_simulation_service.builder.docker_network_builder.subprocess.run",
                   side_effect=_make_run_side_effect()) as mock_run:
            DockerNetworkBuilder().build(_network("sim-net", ["a"]), [_agent("a")])

        connect_calls = [c for c in mock_run.call_args_list
                         if c.args[0][:3] == ["docker", "network", "connect"]]
        assert connect_calls[0].args[0][3] == "sim-net"

    def test_already_connected_container_is_not_an_error(self):
        def _side_effect(cmd, **kwargs):
            result = MagicMock(spec=subprocess.CompletedProcess)
            result.stdout = ""
            if cmd[:3] == ["docker", "network", "ls"]:
                result.returncode = 0
                result.stderr = ""
            elif cmd[:3] == ["docker", "network", "connect"]:
                result.returncode = 1
                result.stderr = "Error response: endpoint with name agent-a already exists in network my-network"
            else:
                result.returncode = 0
                result.stderr = ""
            return result

        with patch("util_simulation_service.builder.docker_network_builder.subprocess.run",
                   side_effect=_side_effect):
            # Should not raise despite returncode=1
            DockerNetworkBuilder().build(_network("my-network", ["agent-a"]), [_agent("agent-a")])

    def test_non_already_connected_error_propagates(self):
        def _side_effect(cmd, **kwargs):
            result = MagicMock(spec=subprocess.CompletedProcess)
            result.stdout = ""
            if cmd[:3] == ["docker", "network", "ls"]:
                result.returncode = 0
                result.stderr = ""
            elif cmd[:3] == ["docker", "network", "connect"]:
                result.returncode = 1
                result.stderr = "Error: no such container: agent-a"
                result.check_returncode.side_effect = subprocess.CalledProcessError(1, cmd)
            else:
                result.returncode = 0
                result.stderr = ""
            return result

        with patch("util_simulation_service.builder.docker_network_builder.subprocess.run",
                   side_effect=_side_effect):
            with pytest.raises(subprocess.CalledProcessError):
                DockerNetworkBuilder().build(_network("my-network", ["agent-a"]), [_agent("agent-a")])

    def test_no_agents_makes_no_connect_calls(self):
        with patch("util_simulation_service.builder.docker_network_builder.subprocess.run",
                   side_effect=_make_run_side_effect()) as mock_run:
            DockerNetworkBuilder().build(_network("my-network", []), [])

        connect_calls = [c for c in mock_run.call_args_list
                         if c.args[0][:3] == ["docker", "network", "connect"]]
        assert len(connect_calls) == 0
