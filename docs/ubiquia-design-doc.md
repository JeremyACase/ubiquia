# Ubiquia Design Document
This is a design document for Ubiquia, a Multi-Agent-System orchestration tool and framework.

## Document Version
- **Version:** 0.1
- **Date:** 2025-05-23
- **Author:** Jeremy Case
- **Reviewed by:** [TBD]

* [Overview](#overview)
* [Goals and Non-Goals](#goals-and-non-goals)
* [Tech Stack](#tech-stack)
* [Architecture](#architecture)
  * [High-Level Architecture Diagram](#high-level-architecture-diagram)
  * [Components](#components)
  * [Message Broker](#message-broker)
  * [Databases](#databases)
  * [Observability](#observability)
  * [Deployment](#deployment)
* [Components and Services](#components-and-services)
  * [Flow Service](#flow-service)
  * [Authorization Service](#authorization-service)
  * [Communication Service](#communication-service)
  * [Executive Service](#executive-service)
  * [Learning Service](#learning-service)
  * [Vectorization Service](#vectorization-service)
* [Data Model](#data-model)
  * [Flow Service](#flow-service)
* [Scalability and Resilience](#scalability-and-resilience)
  * [Scalability](#scalability)
  * [Resilience](#resilience)
* [Security](#security)
  * [Authorization](#authorization)
  * [Data Protection](#data-protection)
  * [Input Validation](#input-validation)
  * [Audit Logging](#audit-logging)
  * [Compliance and Hardening](#compliance-and-hardening)
* [Observability](#observability)
  * [Metrics](#metrics)
  * [Grafana Dashboards](#grafana-dashboards)
  * [YugabyteDB Dashboards](#yugabytedb-dashboards)
  * [Logging](#logging)
* [Deployment Strategy](#deployment-strategy)
  * [Overview](#overview)
  * [Helm Structure](#helm-structure)
* [Tradeoffs](#tradeoffs)


## Overview

**Ubiquia**  is a software system designed to serve as a general-purpose framework for decentralized autonomous situational awareness (ASA.) It is designed with lessons learned from MACHINA as well as recent advancements in Multi Agent Systems (MAS) and Large Language Models (LLMs) such as ChatGPT 3.5.

**Use Case Example:**
Ubiquia can be used to orchestrate a collection of SDA assets--human, LLM, hardware, or otherwise--into an autonomous system capable of surviving a military conflict while still providing invaluable real-time awareness.

---

## Goals and Non-Goals

### Goals
- Enable decentralized coordination between agents across arbitrary scales
- Implement dynamic task reassignment based on current node capacity and priority.
- Enable decentralized coordination between agents to optimize resource distribution.
- Provide observability into agent decisions and system performance.
- Allow new agents to join--or leave--dynamically without downtime.
- Require heterogenous ubiquia agents to be able to realize business logic at runtime.

### Non-Goals
- Ubiquia does not handle physical device provisioning.

## Tech Stack

- **Languages**: Java (Spring Boot 3), Python
- **Database**: YugabyteDB (H2 for testing), QDrant (for LLM's.)
- **Messaging**: Uses distributed database as "message broker" via inbox/outbox pattern
- **Containerization**: Docker + Kubernetes
- **Deployment**: Helm
- **Observability**: Prometheus + Grafana

## Architecture

### High-Level Architecture Diagram
TODO

### Components
- **Flow Service**: Allows for user-defined logic to be registered, instantiated, and ochestrated in runtime.
- **Executive Service**: Coordinates tasks across a network of ubiquia agents
- **Learning Service**: Responsible for updating model weights within agents or even reinforcement learning across LLM agents
- **Mission Logic Service**: TODO: Chat with Justin
- **Communication Service**: Exposes internal service APIs externally - dynamically (such as API's exposed in Flow Service DAG's.)
- **Vectorization Service**: Translates tokenized input into vectors for any LLM Agents on behalf of Flow Service

### Message Broker
- **Database**: The database will be used as a "broker" via the inbox/outbox pattern, allowing for distributed transactions with built-in "queues."

### Databases
- **YugabyteDB (Production)**: Distributed NewSQL database with eventual consistency. Works across Kubernetes clusters.
- **H2 (Testing)**: In-memory database for testing.
- **QDrant**: Vector database for storing LLM history. Intra-cluster; cannot be distributed across Kuberentes clusters.

### Observability
- **Prometheus**: Time-series database that allows for services to emit key-value-pair metadata.
- **Grafana**: Allows for importable (or customizable) dashboards to be built and visualized (primarily using Prometheus data.)

### Deployment
- **Kubernetes**: ubiquia is designed to be run in any Kubernetes environment (e.g., KIND, Rancher, etc.)
- **Helm**: Helm does double-duty as both the package manager and configuration manager for ubiquia.

## Components and Services

### Flow Service
- **Responsibilies**
  - **Agent Communication Language Registration**: The Flow Service will be able to handle "Agent Communication Language" (ACL) registration.
  - **Direct Acylic Graph Registration**: The Flow Service will be able to allow registration of Directed Acyclic Graph workflows comprised of agents and adapters.
  - **Direct Acylic Graph Orchestration**: The Flow Service will double as a Kubernetes operator capable of orchestrating DAG's across Kubernetes clusters dynamically and at runtime.
  - **Schema Validation**: The Flow Service will be able to verify input/output of agents based on an ACL. This is especially important for LLM-based agents.
  - **Vector Database Interface**: For agents that are Large Language Models (LLM's), Flow Service will be able to interface with a vector database on behalf of the LLM.
  - **Back Pressure**: Leveraging queues and the inbox/outbox pattern, Flow Service will provide a "back pressure" endpoint allowing for the Executive Service to be able to actuate Flow Service DAG's across ubiquia Agents to alleviate pressure.
  - **DAG Dataflow**: Flow Service will persist data into the distributed database via a queueing mechanism via the inbox/outbox pattern.
- **API**:
  - **ACL**: The ability to register, query, and delete ACL's
  - **DAGs**: The ability to register, deploy, teardown, query, and delete DAG's
  - **Agent Adapters**: The ability to interface with the adapter deployed for any given agent depending on the type (e.g., Push, Merge, etc.)
  - **Back Pressure**: The ability to query for back pressure for any given adapter.
- **Dependencies**
  - **SQL Database**: Either H2 (for testing) or YugabyteDB.
  - **Vector Database**: A vector database to store vector embeddings for any LLM agents.

### Communications Service
- **Responsibilies**
  - **Reverse Proxy**: The Communications Service will be able to expose services from within a ubiquia agent and Kubernetes agent externally, and be able to do so dynamically (such as will be needed when deploying Flow Service DAG's.)
- **API**:
  - **???**: TODO: Find a way for Comm service to get "Flow Service DAG events" (either via broker or by REST, broker complicates stuff though...) to ensure it can dynamically "surface" internal APIs via Kubernetes

### Executive Service
- **Responsibilies**
  - **Task Distribution**: Actuate Flow Service graphs across a network of ubiquia agents towards optimal usage of compute resources
- **API**
  - **Leader Election**: Synchronize with other executive services across a network of ubiquia agents to do leader election

### Learning Service
- **Responsibilities**
  - **Update Model Weights**: Allows for an API to update model weights for any LLM agents existing in the ubiquia agent

### Vectorization Service
- **Responsibilities**
  - **Vectorize Tokens**: Turn tokenized inputs into Vectors for Flow Service using the LLM models vectorization APIs
- **API**
  - **Vectorize**: Provided a list of tokens and an LLM model, return a list of vectors

## Data Model

### Flow Service

**Flow Service Event**:
```json
{
    "id": "09454cee-a2e4-4c2e-a463-baf5d400ded7",
    "createdAt:" "2022-08-30T21:00:00.000Z",
    "updatedAt:" "2022-08-30T21:00:00.000Z",
    "modelType:" "flowEvent",
    "adapter": {},
    "batchId": "09454cee-a2e4-4c2e-a463-baf5d400ded7",
    "eventTimes": {
        "payloadReceivedTime": "2022-08-30T21:00:00.000Z",
        "payloadSentAgentTime": "2022-08-30T21:00:00.000Z",
        "eventStartTime": "2022-08-30T21:00:00.000Z",
        "eventCompleteTime": "2022-08-30T21:00:00.000Z",
        "egressResponseReceivedTime": "2022-08-30T21:00:00.000Z",
        "agentReponseTime": "2022-08-30T21:00:00.000Z",
        "pollStartedTime": "2022-08-30T21:00:00.000Z",
        "payloadEgressedTime": "2022-08-30T21:00:00.000Z",
    },
    "inputPayload": "I'm the input payload sent to the agent",
    "inputPayloadStamps": [
      {
        "filename": "abc.xyz"
      }
    ],
    "outputPayload": "I'm the out payload the agent responded with",
    "outputPayloadStamps": [
      {
        "pet": "dog"
      }
    ],
    "httpResponseCode": 200,
    "batchId": "09454cee-a2e4-4c2e-a463-baf5d400ded7",
    "inboxOutboxMessages": []
}
```

**Agent Communication Language**:
```json
{
    "id": "09454cee-a2e4-4c2e-a463-baf5d400ded7",
    "createdAt:" "2022-08-30T21:00:00.000Z",
    "updatedAt:" "2022-08-30T21:00:00.000Z",
    "modelType:" "Flow ServiceEvent",
    "version": {
        "major": 1,
        "minor": 2,
        "patch": 3,
    },
    "graphs": [],
    "jsonSchema": "{
      "$schema": "https://json-schema.org/draft/2020-12/schema",
      "title": "Pet",
      "type": "object",
      "properties": {
        "id": {
          "type": "string",
          "description": "Unique identifier for the pet"
        },
        "name": {
          "type": "string",
          "description": "The pet's name"
        },
        "species": {
          "type": "string",
          "enum": ["dog", "cat", "bird", "fish", "other"],
          "description": "Type of animal"
        },
        "age": {
          "type": "integer",
          "minimum": 0,
          "description": "Age of the pet in years"
        },
        "vaccinated": {
          "type": "boolean",
          "description": "Whether the pet is vaccinated"
        }
      },
      "required": ["id", "name", "species"],
      "additionalProperties": false
    }"
}
```

**InboxOutboxMessage**:
```json
{
    "id": "09454cee-a2e4-4c2e-a463-baf5d400ded7",
    "createdAt:" "2022-08-30T21:00:00.000Z",
    "updatedAt:" "2022-08-30T21:00:00.000Z",
    "poppedAt:" null,
    "modelType:" "InboxOutboxMessage",
    "targetAdapter": {},
    "Flow ServiceEvent": {},
    "payload": "I'm the payload!",
}
```

**Backpressure**:
```json
{
  "ingress:" {
    "queuedRecords": 2,
    "queueRatePerMinute": 1.2,
  },
  "egress": {
    "maxOpenMessages": 3,
    "currentOpenMessages": 3,
  }
}
```

### Communications Service: TODO
### Learning Service: TODO
### Mission Logic Service: TODO

## Scalability and Resilience

### Scalability

- **Autoscaling**: Kubernetes Horizontal Pod Autoscaler (HPA) scales ubiquia core microservices based on CPU and custom Prometheus metrics (e.g., request latency, queue depth).
- **In-Transit Compute**: Executive Service will autoscale agents of Flow Service DAG's across ubiquia agents depending on available compute resources and edge requirements (e.g., processing large binaries via edge sensors.)
- **Database Connection Pooling**: Hikari in Spring Boot is used to manage database connection pooling to prevent overload under high concurrency.
- **Inbox/Outbox Queue Design**: Messages flowing over Flow Service DAG's can be treated as queue, where messages can be popped faster or slower depending on compute requirements.
- **Rate Limiting**: API Gateway enforces rate limits per user to protect backend services.
- **Agent Weights**: Ubiquia instances will be configurable with different "weights" (e.g., light, heavy, etc.) so that heterogenous agents of a cluster can be run across anything between edge devices and the cloud.

### Resilience

- **Retry Policy**: HTTP/gRPC calls to internal services use exponential backoff with jitter and a maximum retry budget.
- **Timeouts**: All outbound requests have defined timeouts (e.g., 2s for internal calls, 5s for external).
- **Pod Disruption Budgets**: Ensures a minimum number of replicas remain available during rolling updates or node drains.
- **Operator Notifications**: Production agents will leverage Grafana towards sending notifications to operators when the system changes.

## Security

### Authentication

- **JWT-Based Auth**: All external API requests require a valid JSON Web Token (JWT), issued by the centralized Auth Service.
- **Token Validation**: Middleware validates token signature, expiration, and claims before request handling.
- **Service-to-Service Auth**: Internal microservice calls use mutual TLS and signed service tokens rotated by the service mesh (Istio).

### Authorization

- **Role-Based Access Control (RBAC)**:
  - Each endpoint is annotated with access policies.
  - Roles (e.g., `user`, `admin`) are included in JWT claims and enforced by middleware.
- **Ownership Enforcement**: For resources like user profiles, services check that the caller’s `user_id` matches the resource owner.

### Data Protection

- **Encryption at Rest**: All data stored in PostgreSQL and object storage (e.g., S3) is encrypted using AES-256.
- **Encryption in Transit**: All external and internal communication is TLS 1.3 encrypted.
- **Secrets Management**: All secrets (e.g., API keys, DB creds) are stored in HashiCorp Vault and injected into containers at runtime.

### Input Validation

- All incoming data is validated against strict JSON schemas.
- Server-side validation is enforced even if frontend validation is present.

### Audit Logging

- All sensitive operations (e.g., profile updates, permission changes) are logged with user ID, timestamp, and request context.
- Logs are immutable and stored in a write-only ElasticSearch index with a 90-day retention policy.

### Compliance and Hardening

- Follows OWASP Top 10 guidelines for web application security.
- Containers are scanned regularly for vulnerabilities and patched in CI/CD.
- Uses a hardened base image (Distroless or Alpine) and runs as non-root.
- Security reviews are conducted for all external API integrations and major code changes.

## Observability

Grafana is the central dashboarding and observability platform for this service. Logs, metrics, and traces are aggregated and visualized to support debugging, performance analysis, and alerting.

### Metrics

- **Exported via Prometheus-compatible endpoint** at `/actuator/prometheus` (Spring Boot).
- **Scraped by Prometheus** and visualized in Grafana dashboards.

**Key Metrics:**
- `http_server_requests_seconds_count` – request count by status and path
- `http_server_requests_seconds_bucket` – request latency histogram
- `db_query_duration_seconds` – duration of key DB queries

**Grafana Dashboards:**
- API Latency & Throughput
- Database Query Performance (both Vector database and NewSQL)
- JVM Memory & GC Metrics (if Java)
- Custom per-service KPI panels

**YugabyteDB Dashboards:**
- Latency
- Operations per Second
- Disk Storage
- Slowest queries

---

### Logging

- **Structured logs** in JSON format using Logback + Logstash encoder.
- Collected using **Elasticsearch** and visualized in **Grafana Loki**.

## Deployment Strategy

### Overview

The system will be deployed to a Kubernetes cluster using Helm as the package manager. Each microservice (e.g., Auth Service, Flow Service, Belief State) will have its own Helm chart or will be bundled into a single Helm umbrella chart for the system.

Deployments target three environments: **dev**, **staging**, and **production**. CI/CD pipelines automate deployments using Helm with GitOps-style promotion. Override values for each will be denoted, as will several "weight configurations" for a given ubiquia agent (e.g., "featherweight", "lightweight", "heavyweight", etc.)

---

### Helm Structure

```text
helm/
├── Chart.yaml                  
├── values.yaml                 
├── values/
│   ├── dev.yaml
│   ├── staging.yaml
│   ├── prod.yaml
│   └── configurations/
│       ├── featherweight.yaml
│       ├── lightweight.yaml
│       ├── middleweight.yaml
│       ├── heavyweight.yaml
│       └── ultraweight.yaml
├── templates/
│   ├── core/
│   │   └── service/
│   │       ├── flow-service/
│   │       ├── communications-service/
│   │       ├── executive-service/
│   │       ├── learning-service/
│   │       └── vectorization-service/
|   └── other/
|
```

## Tradeoffs

### Distributed Message Broker via Database (Inbox/Outbox Pattern)

**Pros:**
- Simplifies transactional consistency between message publishing and data persistence.
- Avoids introducing an external broker like Kafka or NATS, reducing ops overhead.
- Enables natural querying over message history and backpressure metrics.
- Enables treating Flow Service DAG's as queues and thus the ability to pop the queue dynanmically.

**Cons:**
- Latency is higher than with dedicated streaming systems.
- Requires careful indexing and pruning strategies for scale.
- Harder to model publish/subscribe semantics or fan-out scenarios without added complexity.

---

### Heterogeneous Agent Runtime (LLMs, Traditional Services, Edge Devices)

**Pros:**
- Flexible architecture allows orchestration of varied compute nodes (LLMs, IoT, APIs).
- DAGs can span cloud and edge environments seamlessly.
- Flow Service abstraction enables runtime orchestration without recompilation.

**Cons:**
- Significant coordination overhead across diverse runtimes.
- Testing and debugging multi-type agents is more complex.
- Requires detailed domain schema validation to maintain type safety across agents.

---

### Helm and Kubernetes as Orchestration Backbone

**Pros:**
- Helm provides repeatable, environment-specific deployments.
- Kubernetes supports horizontal scaling and resilient runtime orchestration.
- Helm values system enables “agent weight” tuning for flexible resource targeting.

**Cons:**
- Helm charts can become hard to manage at scale, especially across highly dynamic DAGs.
- Kubernetes environments vary widely (e.g., KIND vs EKS), making edge deployments tricky.
- Requires deeper Kubernetes expertise for agent developers and ops teams.

---

### Use of YugabyteDB for Core State and Messaging

**Pros:**
- Global consistency model with distributed SQL semantics.
- Well-suited for inbox/outbox-based workflows and relational DAG tracing.
- Easily scales across Kubernetes clusters.

**Cons:**
- Operationally heavier than PostgreSQL; requires more resources.
- Some SQL extensions (e.g., full-text search) are limited or incompatible.
- Must manage potential cross-region latency in multi-cluster deployments.

---

### Vector DB (QDrant) for LLM History and Embeddings

**Pros:**
- Lightweight and fast approximate nearest-neighbor search engine.
- Simple integration with LLM agents for memory, search, and context caching.
- Useful for RLHF and prompt optimization strategies.

**Cons:**
- QDrant is not fully distributed across Kubernetes clusters (intra-cluster only).
- Adds another stateful dependency that must be maintained separately.
- Embedding consistency depends on the vectorizer used at ingestion time.

---

### Runtime DAG Orchestration with Flow Service

**Pros:**
- DAGs can be authored and deployed at runtime without needing redeploys.
- Enables dynamic agent and adapter instantiation based on load and mission profile.
- Backpressure system allows compute-aware routing and rebalancing.

**Cons:**
- Runtime DAG orchestration increases surface area for debugging and recovery.
- Requires strong schema enforcement and introspection tooling.
- Distributed failures in a DAG may be harder to diagnose than in statically defined pipelines.

---

### Vectorization-as-a-Service

**Pros:**
- Centralized vectorization abstracts away model internals and supports multiple LLMs.
- Standardizes vector generation for use in QDrant, logging, or downstream planning.
- Can be scaled and optimized independently of the Flow Service pipeline.

**Cons:**
- Adds latency if called synchronously during event processing.
- Introduces a single point of failure in LLM-dependent workflows.
- Versioning and embedding drift must be tracked and reconciled over time.

## Contributors
* __Jeremy Case__: jeremycase@odysseyconsult.com