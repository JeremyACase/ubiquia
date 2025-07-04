# Ubiquia

> **A runtime-compiled, DAG-orchestrated, schema-driven platform for building intelligent agent ecosystems — at production scale.**

Ubiquia is a **domain-agnostic, production-grade Multi-Agent Orchestration System**. It lets you declaratively define agent communication protocols and workflows, then compiles and deploys them into a **live, resilient, and self-validating distributed system** — all from JSON schemas and YAML-ized Directed Acyclic Graphs (DAG's).

---

## 🌟 Why Ubiquia?

Ubiquia is not another workflow engine or LLM wrapper. It's a **foundational infrastructure layer** designed to support:

- **Typed agent communication** (via JSON Schema)
- **Runtime deployment** of distributed, RESTful belief states
- **Persistent, self-evolving DAGs** of intelligent agents
- **Schema-validated I/O contracts between services**
- **Distributed system messaging with SQL-level guarantees**

Ubiquia enables a future where **LLMs**, **symbolic agents**, and **software systems** can collaborate in validated, evolving ecosystems — with no glue code.

---

## 🔥 Key Innovations

- 🧠 **Schema-to-Service Compilation**  
  Generate and deploy fully-typed REST APIs from JSON Schema definitions. No scaffolding. No boilerplate. Just the schema.

- 🗺️ **Declarative DAGs → Kubernetes Agents**  
  Define a multi-agent system with a single YAML file. Ubiquia reads the DAG and deploys it to Kubernetes — each node a live, stateful microservice.

- 🌍 **Cross-Cluster DAG Communication**  
  DAGs can span **multiple Kubernetes clusters**, enabling agents to operate across physical or cloud boundaries. This supports:
  - **Resilient compute**: workloads can route around degraded or partitioned clusters  
  - **Topology-aware orchestration**: agents and DAG nodes can execute **close to data, users, or compute availability zones**

- 🧬 **Distributed Belief State via Configurable SQL Backend**  
  Agents share a global belief state backed by a **relational SQL database**, with support for either:
  - **YugabyteDB** for distributed, horizontally scalable, highly available environments  
  - **H2** for lightweight, in-memory or single-node scenarios such as local dev, testing, or compute-constrained environments

- 🧾 **Runtime Schema Enforcement**  
  Every I/O channel between agents is validated in real time. No brittle prompt logic, no undefined behavior — just contract-based communication.

- 🔄 **Recursive System Evolution** *(Experimental)*  
  DAGs and belief schemas can be evolved by agents themselves, opening the door to recursive intelligence: **AutoGPT, but type-safe and production-ready.**

- 🧰 **Spring Boot + Helm + Gradle**  
  Built on rock-solid Java infrastructure and Kubernetes-native deployment patterns. DevOps-friendly and cloud-ready.

---

## Table of Contents

* [Quickstart](#quickstart)
  * [Quickstart: Requirements](#quick-start-requirements)
  * [Quickstart: Scripts](#quick-start-scripts)
  * [Quickstart: Scripts - One-Time Setup](#quick-start-scripts-one-time-setup)
* [Getting Started](#getting-started)
  * [Getting Started: Requirements](#getting-started-requirements)
  * [Getting Started: Helm Repo](#getting-started-helm-repo)
  * [Getting Started: Installation](#getting-started-installation)
  * [Getting Started: Project Overview](#getting-started-project-overview)
* [For Devs](#for-devs)
  * [For Devs: Building Ubiquia](#for-devs-building-ubiquia)
  * [For Devs: Building Subprojects](#for-devs-building-subprojects)
* [Contributors](#contributors)
* [Who Is This For?](#who-is-this-for)
* [License](#license)

## Quickstart
The quickest way to get up and running with Ubiquia is to follow this section.

### Quickstart: Requirements
Before running Ubiquia, make sure the following tools are installed:

| Tool        | Minimum Version | Install Link |
|-------------|-----------------|--------------|
| Helm        | 3.12.0          | [helm.sh/docs](https://helm.sh/docs/intro/install/) |
| KIND        | 0.20.0          | [kind.sigs.k8s.io](https://kind.sigs.k8s.io/docs/user/quick-start/#installing-with-go-install) |
| Kubectl     | 1.27.0          | [kubernetes.io](https://kubernetes.io/docs/tasks/tools/) |
| Docker      | 24.0.0          | [docs.docker.com](https://docs.docker.com/engine/install/) |

### Quickstart: Scripts
Some convenience scripts are provided to users in this repo to get users up and running. ***The scripts must be executed in the root Ubiquia directory.***

### Quickstart: Scripts - One-Time Setup
These scripts should only ever need to be run once.
```bash
$ ./scripts/devs/helm-repo-setup.sh
```

### Quickstart: Scripts - Recurring Setup
These scripts need to be run whenever you want to do a fresh install of Ubiquia in a new KIND cluster.

```bash
$ ./scripts/devs/install-ubiquia-into-kind.sh
```

After invoking the script and a successful installation, Helm will output to console how to interface with the newly-installed Ubiquia agent.

### Quickstart: Deleting Ubiquia Cluster
If you ran the above script to install Ubiquia into KIND and want a completely fresh start, you can delete the KIND Kubernetes cluster

```bash
$ kind delete clusters ubiquia-agent-0
```

Now you can re-run the installation script with a fresh Kubernetes cluster!


## Getting Started

### Getting Started: Development Requirements
Before running Ubiquia, ensure the following tools are installed.

#### Installation Tools

| Tool        | Minimum Version | Install Link |
|-------------|-----------------|--------------|
| Helm        | 3.12.0          | [helm.sh/docs](https://helm.sh/docs/intro/install/) |
| Kubectl     | 1.27.0          | [kubernetes.io](https://kubernetes.io/docs/tasks/tools/) |
| Kubernetes  | 1.27.0          | [kubernetes.io](https://kubernetes.io/docs/setup/) |

#### Development Tools

| Tool        | Minimum Version | Install Link |
|-------------|-----------------|--------------|
| Docker      | 24.0.0          | [docs.docker.com](https://docs.docker.com/engine/install/) |
| KIND        | 0.20.0          | [kind.sigs.k8s.io](https://kind.sigs.k8s.io/docs/user/quick-start/#installing-with-go-install) |
| OpenJDK     | 21              | [jdk.java.net/21](https://jdk.java.net/21/) or a preferred distribution |


### Getting Started: Installation
Ubiquia should be able to be installed into any Kubernetes environment using Helm as a package manager. Ubiquia's configurable values are listed in the values.yaml file. Specific configurations are available to devs that override a subset of these values. They can be found in 'helm/configurations/'.

Example Ubiquia Installation
```bash
$ helm install ubiquia ubiquia-helm --values helm/configurations/featherwweight.yaml -n ubiquia
```

### Getting Started: Project Overview
Ubiquia is a modular multi-agent orchestration platform designed for scalable, belief-driven AI systems running on Kubernetes.

To ensure modularity, clarity, and maintainability, the codebase is divided into subprojects. Each will eventually have its own README and design documents — some already do, and others are coming soon.

```text
root/
├── build.gradle            # Top-level Gradle build
├── settings.gradle         # Declares subprojects
├── gradle.properties       # Centralized version declarations
├── helm/                   # Helm chart for Kubernetes deployment
├── scripts/                # Dev and ops automation scripts
├── docs/                   # System-level documentation and diagrams
├── config/                 # Project-wide config (e.g., Checkstyle rules) or development config separate of Helm
├── common/ 
│   ├── library/            # Shared APIs and libraries used across services
│   └── model/              # Shared model definitions and database entities  
├── core/
│   └── service/            # Core services that will run as as K8s microservices
└──
```

## For Devs
This section will contain a handful of useful commands, topics, concepts, or otherwise for developers using the Ubiquia framework.

### For Devs: Building Ubiquia
The entire Ubiquia project can be built by invoking Gradle. This will task each subproject to build.
```bash
$ ./gradlew clean build
```

### For Devs: Building Subprojects
The subprojects of Ubiquia can be built by invoking specific subprojects via Gradle with a command.

Generic Example:
```bash
$ ./gradlew :<folder>:<folder>:<subproject>:<command>
```

Concrete Example:
```bash
$ ./gradlew :core:service:flow-service:build
```

## Who Is This For?

- **ML engineers** who want LLM agents with schema validation  
- **Backend developers** who want to design systems with declarative intent and runtime validation  
- **Systems engineers** building distributed or autonomous orchestration platforms  
- **Scientists & researchers** modeling intelligent ecosystems or recursive planners  
- **DoD/IC technologists** looking to modernize simulation, orchestration, or planning infrastructure

---

## Contributors
* __Jeremy Case__: jeremycase@odysseyconsult.com

## License

Apache 2.0 — Open source, extensible, and ready for real-world deployment.