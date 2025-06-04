# Ubiquia

Welcome to Ubiquia — a domain-agnostic, production-grade Multi-Agent Orchestration System built from the ground up for dynamic, runtime data flows, resilient compute, and robust distributed belief state sharing.

## Table of Contents

* [Ubiquia Overview](#ubiquia-overview)
    * [About](#about)
* [Quickstart](#quickstart)
    * [Quickstart: Requirements](#quick-start-requirements)
    * [Quickstart: Scripts](#quick-start-scripts)
    * [Quickstart: Scripts - One-Time Setup](#quick-start-scripts-one-time-setup)
* [Getting Started](#getting-started)
    * [Getting Started: Requirements](#getting-started-requirements)
    * [Getting Started: Helm Repo](#getting-started-helm-repo)
    * [Getting Started: Installation](#getting-started-installation)
* [Contributors](#contributors)

## Ubiquia Overview

### About
Ubiquia is a production-grade multi-agent system (MAS) orchestration tool that brings the power of dynamic, runtime agent coordination to real-world deployments. Key features include:

- 🛠️ **Production-Ready MAS Orchestration:** A carefully architected orchestration engine designed to handle dynamic, real-time data flows and resilient agent networks.
- 🌐 **Distributed Belief State with YugabyteDB:** Built on YugabyteDB, a resilient, distributed SQL database that underpins Ubiquia’s shared global belief state, ensuring eventual consistency and high availability.
- 🔄 **Dynamic Domain Ontology Realization:** Ubiquia can derive bidirectional, relational database schemas--domain ontologies--directly from JSON schemas at runtime, enabling rapid modeling that adapts to evolving requirements.
- 🔍 **Runtime Schema Validation:** Employs JSON Schema validation to ensure that all data exchanged between agents and the orchestration layer adheres to well-defined, dynamically loaded schemas.
- 🌐 **Automatic RESTful API Generation:** Automatically creates a RESTful server that exposes these domain ontologies as APIs, providing seamless CRUD operations for runtime-managed data.
- 🚀 **Kubernetes-Native Deployment:** Ubiquia is designed to run seamlessly in containerized environments orchestrated by Kubernetes, making it easy to deploy, scale, and maintain in real-world production clusters.
- 🔄 **Resilience to Network Partitions:** Engineered to gracefully handle transient network partitions, ensuring that agents can rejoin the system and maintain a consistent belief state.
- ☕ **Spring Boot Foundation:** Leverages the battle-tested Spring Boot ecosystem for robust microservice capabilities, including dependency injection, lifecycle management, and secure configuration.

Together, these features make Ubiquia a production-ready, highly reliable MAS orchestration tool that is both developer-friendly and operationally robust.

## Quickstart
The quickest way to get up and running with Ubiquia is to follow this section.

### Quickstart: Requirements
Before running Ubiquia, there are a few requirements that must be met.

- [Helm](https://helm.sh/docs/intro/install/) has been installed 
- [KIND](https://kind.sigs.k8s.io/docs/user/quick-start/#installing-with-go-install) has been installed
- [Kubectl](https://kubernetes.io/docs/tasks/tools/) has been installed
- [Docker](https://docs.docker.com/engine/install/) has been installed (required by KIND)

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


## Getting Started

### Getting Started: Requirements
Before running Ubiquia, there are a few requirements that must be met.

- [Helm](https://helm.sh/docs/intro/install/) has been installed 
- [Kubectl](https://kubernetes.io/docs/tasks/tools/) has been installed


### Getting Started: Installation
Ubiquia should be able to be installed into any Kubernetes environment using Helm as a package manager. Ubiquia's configurable values are listed in the values.yaml file. Specific configurations are available to devs that override a subset of these values. They can be found in 'helm/configurations/'.

Example Ubiquia Installation
```bash
$ helm install ubiquia ubiquia-helm --values helm/configurations/featherwweight.yaml -n ubiquia
```

## Contributors
* __Jeremy Case__: jeremycase@odysseyconsult.com
