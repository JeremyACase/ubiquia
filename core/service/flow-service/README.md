
# 🌀 `flow-service`

> **A Kubernetes-native component deployment and coordination engine for DAG-based orchestration in Ubiquia.**

The `flow-service` is a core microservice in Ubiquia that handles **component lifecycle, communication, and orchestration logic** via Directed Acyclic Graphs (DAG's). It exposes RESTful endpoints to deploy, manage, and inspect components and adapters within a live Kubernetes cluster, enabling dynamic orchestration of multi-component workflows as well as enabling clients to query, inspect, and trace data as it **flows** across Ubiquia DAG's. 

---

## 🚀 Responsibilities

- 🧭 Interpret and deploy **Directed Acyclic Graphs (DAGs)** that define agentic data workflows  
- ⚙️ Dynamically configure DAG behavior via dev-provided flags (e.g., adapter types, property overrides, etc.)  
- 🧠 Deploy components as DAG nodes with schema-validated I/O contracts  
- 🪝 Instantiate both **component-based** and **agentless** adapters for routing, queuing, or transforming data  
- 🔌 Expose REST APIs via adapters — each adapter type (e.g., `publish`, `subscribe`, `poll`) defines its own interaction model  
- 📡 Register and inspect Agent Communication Languages (ACLs) for type-safe component communication  
- 📘 Serve dynamic, strongly-typed REST endpoints defined by the DAG and ACLs  
- 📈 Expose an API for querying **flow events**, enabling inspection of messages as they propagate (flow) through DAGs  
- 🧩 Interface with Kubernetes, SQL-backed belief state, and schema registries

---

## 📦 Features

- **Agent Lifecycle Management**  
  Deploy, start, stop, and inspect components as DAG nodes via `AgentController`

- **Adapter System**  
  Each edge in the DAG is represented as an **adapter** (e.g. `PushAdapter`, `MergeAdapter`, `SubscribeAdapter`, etc.) responsible for message transport and control logic

- **Dynamic DAG Realization**  
  DAGs defined in YAML are materialized into active services through a series of controller endpoints and dependency injection via Spring Boot

- **Type-Safe Agent Communication**  
  Agent I/O contracts are enforced through registered JSON Schemas called **Agent Communication Languages** (ACLs)

---

## 🛠️ Building the Service

```bash
./gradlew clean :core:service:flow-service:build
```

### 🏃 Running the Service (Locally)

To run `flow-service` locally with Spring Boot:

```bash
./gradlew :core:service:flow-service:bootRun
```

## 🧰 Project Structure

```text
src/main/java/org/ubiquia/core/flow
├── component/             # Adapters and other code living as Spring Boot components
├── config/                # Data structures representing configuration
├── controller/            # REST controllers for components, adapters, and schemas
├── interfaces/            # Java Interfaces for use internally by the Flow Service
├── model/                 # Models for use internally by the Flow Service
├── repository/            # JPA repositories for the flow-service to communicate with the back-end database
├── service/               # Services that largely map to design patterns for use within the Flow Service
├── Application.java       # Spring Boot entrypoint
├── Config.java            # Dependency injection setup for DAG orchestration
├── resources/             # Resources to be packaged with the application 
├── build.gradle           # The Gradle build for the Flow Service 
├── Dockerfile             # The Dockerfile that defines how this is containerized in devops pipelines 
└──
```

---

## 🧬 Relationship to Ubiquia

`flow-service` is one of the core orchestration microservices inside Ubiquia. It realizes DAGs into Kubernetes-native components and adapters, enforcing ACL contracts and routing messages based on YAML-defined topologies and schema metadata.

---

## 📚 Exploring the API (Swagger UI)

The easiest way to explore `flow-service` is through its built-in **Swagger UI**, which documents all available endpoints, request/response types, and schemas in real time.

Once the service is running, open your browser to:

```
http://localhost:8080/swagger-ui/index.html
```

You can use this UI to:
- Test endpoints interactively (e.g., deploy components, register graphs, register ACLs, etc.)
- View schema-based validation for requests
- Inspect all available controllers and routes

Make sure you're running the service locally using:

```bash
./gradlew :core:service:flow-service:bootRun
```

## 📜 License

This module is part of Ubiquia and licensed under [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0).
