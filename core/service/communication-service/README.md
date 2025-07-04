
# 🌐 `communication-service`

> **The unified reverse proxy and routing gateway for Ubiquia microservices and DAG-deployed agents.**

The `communication-service` is a core component of Ubiquia responsible for routing all external and internal traffic through a unified, dynamic reverse proxy. It acts as the **entry point into the Ubiquia ecosystem**, forwarding requests to core microservices and any dynamically-deployed DAG nodes — including agents and adapters — based on real-time DAG configuration.

---

## 🚀 Responsibilities

- 🌉 Reverse proxy for all **core Ubiquia microservices** (e.g., flow-service, belief-state-service)  
- 🛰️ Dynamically proxy to **agent and adapter endpoints** discovered via DAG YAML definitions  
- 🧠 Maintain internal registries of available services and DAG-deployed entities  
- 🔄 Route traffic to agent controllers, belief state APIs, and adapter endpoints based on service roles  
- 🔌 Provide proxy controller interfaces for use by UI layers or external integrations

---

## 📦 Features

- **Service Proxy Configurations**  
  Automatically configures and maintains reverse proxy targets for each major microservice via Spring-based configuration classes

- **Dynamic Adapter and Agent Proxying**  
  Proxies requests to adapter and agent services discovered from DAG deployments using custom managers and pollers

- **Controller Proxy Interfaces**  
  Proxy classes like `GraphControllerProxy`, `AdapterReverseProxyController`, and `BeliefStateGeneratorControllerProxy` expose simplified paths into downstream services

- **Cluster-Aware Routing**  
  Routes requests within the active Kubernetes namespace to agents, adapters, or microservices regardless of origin

- **Resilient Architecture**  
  Handles unavailability, retry logic, and graceful degradation when reverse targets are temporarily offline

---

## 🛠️ Building the Service

```bash
./gradlew clean :core:service:communication-service:build
```

### 🏃 Running the Service (Locally)

```bash
./gradlew :core:service:communication-service:bootRun
```

Once running, you can hit the root proxy at:

```
http://localhost:8080/
```

Proxy routes will be dynamically enabled based on DAG deployments and service discovery.

---

## 🧰 Project Structure

```text
src/main/java/org/ubiquia/core/communication
├── config/                # Spring configuration for service-level proxies
├── controller/            # Reverse proxy controllers for agents, adapters, and microservices
│   ├── belief/            # Belief-state specific proxy endpoints
│   └── flow/              # Flow service and adapter routing
├── interfaces/            # Interfaces used by proxy controller abstractions
├── service/
│   ├── io/                # Pollers that track deployed graphs and endpoints
│   └── manager/           # Adapter and agent proxy managers for dynamic routing
├── Application.java       # Spring Boot entrypoint
├── Config.java            # Global DI and system configuration
```

---

## 🔗 Usage Context

This service is deployed as the **primary ingress gateway** for Ubiquia:

- Used by UIs and clients to interact with belief states, agents, and adapters  
- Dynamically adapts to changing DAG topologies and deployment configurations  
- Supports multi-agent DAG ecosystems with runtime-adaptive routing

---

## 📜 License

This module is part of Ubiquia and licensed under [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0).
