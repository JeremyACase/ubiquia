import logging
import pathlib
import subprocess
import tempfile

from util_simulation_service.builder.agent_builder import AgentBuilder
from util_simulation_service.model.agent import Agent
from util_simulation_service.model.agent_mode import AgentMode

logger = logging.getLogger(__name__)

_NAMESPACE = "ubiquia"
_HELM_RELEASE = "ubiquia"
_IMAGE_NAME = "ubiquia/core-flow-service"
_NODE_PORT = 30080

_KIND_CONFIG_TEMPLATE = """\
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    extraPortMappings:
      - containerPort: {node_port}
        hostPort: {host_port}
        listenAddress: "0.0.0.0"
        protocol: TCP
"""


class KindAgentBuilder(AgentBuilder):
    """Provisions a Ubiquia agent as a featherweight Helm install into a KIND cluster.

    Mirrors the behaviour of install-ubiquia-into-kind.sh with featherweight-dev.yaml:
      - Creates a dedicated KIND cluster per agent with a unique host port mapping.
      - Builds and loads the core-flow-service Docker image into the cluster.
      - Applies RBAC and persistent volume manifests.
      - Helm installs the Ubiquia chart with the featherweight (H2, no MinIO) values.

    Each successive call to build() claims the next port starting from base_port.
    """

    def __init__(self, repo_root: pathlib.Path, base_port: int = 8080):
        self._repo_root = repo_root
        self._next_port = base_port

    def build(self, agent_name: str) -> Agent:
        host_port = self._next_port
        self._next_port += 1
        cluster_name = f"ubiquia-{agent_name}"

        logger.info("Provisioning KIND agent '%s' (cluster=%s, port=%d).", agent_name, cluster_name, host_port)

        self._ensure_cluster(cluster_name, host_port)
        self._ensure_namespace(cluster_name)
        self._apply_k8s_resources(cluster_name)
        self._build_and_load_image(cluster_name)
        self._helm_install(cluster_name)

        base_url = f"http://localhost:{host_port}"
        logger.info("KIND agent '%s' ready at %s.", agent_name, base_url)
        return Agent(name=agent_name, base_url=base_url, mode=AgentMode.KIND)

    def _ensure_cluster(self, cluster_name: str, host_port: int) -> None:
        result = subprocess.run(
            ["kind", "get", "clusters"],
            check=True,
            capture_output=True,
            text=True,
        )
        if cluster_name in result.stdout.splitlines():
            logger.info("KIND cluster '%s' already exists — skipping creation.", cluster_name)
            return

        logger.info("Creating KIND cluster '%s' with host port %d → node port %d.", cluster_name, host_port, _NODE_PORT)
        config_yaml = _KIND_CONFIG_TEMPLATE.format(node_port=_NODE_PORT, host_port=host_port)
        config_path = pathlib.Path(tempfile.mkdtemp()) / "kind_config.yaml"
        config_path.write_text(config_yaml)

        subprocess.run(
            ["kind", "create", "cluster", "--name", cluster_name, "--config", str(config_path)],
            check=True,
        )

    def _ensure_namespace(self, cluster_name: str) -> None:
        context = f"kind-{cluster_name}"
        result = subprocess.run(
            ["kubectl", "--context", context, "get", "namespace", _NAMESPACE],
            capture_output=True,
        )
        if result.returncode != 0:
            logger.info("Creating namespace '%s' in cluster '%s'.", _NAMESPACE, cluster_name)
            subprocess.run(
                ["kubectl", "--context", context, "create", "namespace", _NAMESPACE],
                check=True,
            )
        else:
            logger.info("Namespace '%s' already exists.", _NAMESPACE)

    def _apply_k8s_resources(self, cluster_name: str) -> None:
        logger.info("Applying RBAC and persistent volume manifests to '%s'.", cluster_name)
        context = f"kind-{cluster_name}"
        node = f"{cluster_name}-control-plane"
        config_dev = self._repo_root / "deploy" / "config" / "dev"

        subprocess.run(
            ["docker", "exec", node, "mkdir", "-p", "/mnt/data/belief-state-jars"],
            check=True,
        )
        subprocess.run(
            ["kubectl", "--context", context, "apply",
             "-f", str(config_dev / "ubiquia_dev_service_account.yaml"), "-n", _NAMESPACE],
            check=True,
        )
        subprocess.run(
            ["kubectl", "--context", context, "apply",
             "-f", str(config_dev / "ubiquia_dev_kind_pv.yaml"), "-n", _NAMESPACE],
            check=True,
        )

    def _build_and_load_image(self, cluster_name: str) -> None:
        logger.info("Building Docker image '%s' and loading into KIND cluster '%s'.", _IMAGE_NAME, cluster_name)
        service_dir = self._repo_root / "services" / "core" / "java" / "core-flow-service"
        subprocess.run(
            ["docker", "build", "--build-arg", "OPENJDK_VERSION=21",
             "-t", f"{_IMAGE_NAME}:latest", str(service_dir)],
            check=True,
        )
        subprocess.run(
            ["kind", "load", "docker-image", f"{_IMAGE_NAME}:latest", "--name", cluster_name],
            check=True,
        )

    def _helm_install(self, cluster_name: str) -> None:
        context = f"kind-{cluster_name}"
        helm_dir = self._repo_root / "deploy" / "helm"
        values_file = helm_dir / "configurations" / "dev" / "featherweight-dev.yaml"

        logger.info("Updating Helm dependencies.")
        subprocess.run(["helm", "dependency", "update", str(helm_dir)], check=True)

        result = subprocess.run(
            ["helm", "--kube-context", context, "status", _HELM_RELEASE, "-n", _NAMESPACE],
            capture_output=True,
        )
        if result.returncode == 0:
            logger.info("Helm release '%s' exists — upgrading.", _HELM_RELEASE)
            subprocess.run(
                ["helm", "--kube-context", context, "upgrade", _HELM_RELEASE, str(helm_dir),
                 "--values", str(values_file), "-n", _NAMESPACE],
                check=True,
            )
        else:
            logger.info("Installing Helm release '%s' with featherweight values.", _HELM_RELEASE)
            subprocess.run(
                ["helm", "--kube-context", context, "install", _HELM_RELEASE, str(helm_dir),
                 "--values", str(values_file), "-n", _NAMESPACE],
                check=True,
            )
