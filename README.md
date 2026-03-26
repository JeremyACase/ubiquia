# Ubiquia

> **Ubiquia is a distributed, schema-driven multi-node orchestration system. Define DAGs declaratively, and Ubiquia automatically deploys and wires them into a live Kubernetes ecosystem—with optional belief state services for distributed, persistent, queryable shared context. All data is governed by schemas, ensuring reliable structure across all nodes and components—including LLMs.**

---

## 🌟 Why Ubiquia?

The defense industry has a deep structural problem: capability vendors are forced to anticipate the operational environment at development time. In contested, degraded, and rapidly changing conditions, that is impossible. The result is brittle, vendor-locked systems that cannot adapt when the network degrades, infrastructure is lost, or mission objectives change.

**Ubiquia solves this by decoupling capability development from operational control.**

Vendors define what their hardware or software can do. Operators define objectives. **Ubiquia handles everything in between**—deploying workflows, routing compute, replicating data, and reforming the network in real time as conditions change and nodes are lost.

This means:
- capability developers do **not** need to know the runtime topology in advance
- operators do **not** need to hand-wire brittle integrations under changing mission conditions
- systems can continue adapting even as infrastructure degrades or fragments

Ubiquia provides a resilient, self-organizing software platform for distributed operations.

- 🔁 **Composable DAGs**  
  Describe workflows as YAML-based DAGs. Ubiquia spins up nodes, components, belief states, and communication services on demand.

- 📦 **Schema-to-Belief Pipelines**  
  Provide a **Domain Data Contract (DDC)** (i.e., JSON Schema), and Ubiquia will deploy a **RESTful Belief State service** with:  
  - Distributed persistence  
  - A fully-typed REST API generated from your schema  
  - A relational back-end  
  - An expressive, intuitive query API with paginated results
  - Real-time ingestion of normalized relational data

- 🧬 **Schema-Driven Stability**  
  Prevent long-term degeneration in distributed multi-node systems. Every I/O channel is bound to a contract defined in JSON Schema, keeping nodes—including LLMs—grounded in clean, structured, machine-validated data.

- ⚙️ **Kubernetes-Native by Design**  
  Helm-first deployments, Prometheus & Micrometer observability baked in. Built for production from day one.

- 🌍 **Cross-Cluster DAG Communication**  
  DAGs can span **multiple Kubernetes clusters**, enabling nodes to operate across physical and cloud boundaries:  
  - **Resilient Compute**: workloads can route around degraded or partitioned clusters  
  - **Topology-Aware Execution**: deploy nodes near data, users, or available compute zones  
  - **Autonomous Reformation**: workflows can continue adapting as nodes fail, disconnect, or rejoin

- 🛡️ **Built for Contested Environments**  
  Ubiquia is designed for environments where topology, connectivity, and available compute cannot be assumed ahead of time. It continuously manages those realities at runtime rather than forcing vendors or operators to hard-code around them up front.

---

Ubiquia is not a clean-sheet concept. It is the generalization of **MACHINA**, a fielded system already used for space domain awareness sensor orchestration at operational scale. Ubiquia carries forward the lessons learned from building and operating MACHINA in the real world, while evolving the architecture into a more general, production-ready platform.

Unlike most MAS (Multi-Agent System) frameworks, **Ubiquia is designed from the ground up for real-world, production-grade integration in dynamic and contested environments**. It combines formal schema enforcement with robust deployment tooling, dynamic service generation, autonomous reconfiguration, and cross-cluster communication—bridging the gap between research prototypes and hardened operational systems.

---

## Getting Started: Project Overview

Ubiquia is a modular multi-node orchestration platform designed for scalable, belief-driven AI systems running on Kubernetes.

At a high level, Ubiquia separates three concerns that are too often tightly coupled in traditional systems:

1. **Capability development** — vendors define what their software or hardware can do  
2. **Operational intent** — operators define mission objectives and desired outcomes  
3. **Runtime orchestration** — Ubiquia determines how work, data, and communication should be coordinated in the environment that actually exists

This separation allows Ubiquia to adapt to changing runtime conditions without requiring every capability provider to predict network topology, compute availability, or degradation scenarios at development time.

To ensure modularity, clarity, and maintainability, the codebase is divided into subprojects. Each will eventually have its own README and design documents — some already do, and others are coming soon.

```text
root/
├── build.gradle            # Top-level Gradle build
├── settings.gradle         # Declares subprojects
├── gradle.properties       # Centralized version declarations
├── deploy/                 # Deployment files
│   ├── config/             # Any configuration for Ubiquia existing outside of Helm
│   └── helm/               # Helm manifests to deploy Ubiquia into Kubernetes
├── tools/                  # Dev and ops automation scripts
├── docs/                   # System-level documentation and diagrams
├── config/                 # Project-wide config (e.g., Checkstyle rules) or development config separate from Helm
├── common/java
│   ├── library/            # Shared APIs and libraries used across services
│   ├── test/               # Shared libraries for internal e2e tests
│   └── model/              # Shared model definitions and database entities
├── services/
│   ├── dag/                # Services related to DAGs that ship with Ubiquia
│   ├── test/               # Test microservices that run in Helm tests
│   └── core/               # Core services that will run as K8s microservices
└──
```

## Domain Ontology

Ubiquia requires a "Domain Ontology" to be registered before it can manage workflows for a particular domain. Generally, a domain ontology is some metadata around two other items: a **Domain Data Contract (DDC)** that defines the models and schemas of that domain, and a list of **Directed Acyclic Graphs (DAGs)** that define various workflows within that domain. 

### Domain Ontology: Domain Data Contracts

Ubiquia revolves around the idea that schemas are necessary to prevent multi-agent systems from collapsing under their own weight. To battle entropy at scale, Ubiquia requires a **Domain Data Contract (DDC)** before orchestrating any DAGs or Belief States. DDCs are themselves really minimal metadata around the [JSON Schema](https://json-schema.org/) specification.

```yaml
name: pets
modelType: DomainOntology
author: Jeremy Case
description: The "Hello World" of Domain Ontologies in Ubiquia, but with Pets!

# ontologies require semantic versions
version:
  major: 1
  minor: 2
  patch: 3

# These are any tags a developer would like attached to their DAG
tags:
  - key: testTagKey1
    value: testTagValue1
  - key: testTagKey2
    value: testTagValue2
  - key: testTagKey3
    value: testTagValue3

domainDataContract:

graphs:
```

#### Domain Data Contracts: Defining DDCs

**Domain Data Contractrs (DDCs)** in Ubiquia are (JSON Schemas)[https://json-schema.org/] formatted in YAML that describe the models/definitions/schemas for a domain in Ubiquia. They can be relational, polymorphic, or flat--Ubiquia will handle the relationship in any of those cases. 

Here's an example DDC, defined within a Domain Ontology's "domainDataContract" section

```yaml
domainDataContract:

  modelType: DomainDataContract

  schema:
    type: object

    definitions:

      ColorType:
        type: string
        enum:
          - BLUE
          - GREEN
          - BROWN
          - BLACK
          - WHITE
          - GRAY

      Name:
        description: A model with some name information.
        type: object
        properties:
          firstName:
            type: string
          lastName:
            type: string
        required:
          - firstName

      BaseModel:
        description: A base model.
        type: object
        properties:
          info:
            type: string
            maxLength: 100
            default: Just some info about this model.

      BinaryFile:
        description: A model representing raw binary file data.
        type: object
        properties:
          data:
            type: string
            format: byte
            description: Base64-encoded binary data.
        required:
          - data

      AdoptionTransaction:
        description: A transaction of an adoption.
        type: object
        allOf:
          - $ref: "#/definitions/BaseModel"
        properties:
          owner:
            $ref: "#/definitions/Person"
          pet:
            $ref: "#/definitions/Animal"

      ClassificationResult:
        description: The result of our ML model making a classification inference.
        type: object
        allOf:
          - $ref: "#/definitions/BaseModel"
        properties:
          class:
            type: string
            description: The class label predicted by the model.
          confidence:
            type: number
            format: float
            minimum: 0.0
            maximum: 1.0
            description: The confidence score (between 0 and 1) for the predicted class.
        required:
          - class
          - confidence

      Person:
        description: A model of a person.
        type: object
        allOf:
          - $ref: "#/definitions/BaseModel"
        properties:
          hairColor:
            $ref: "#/definitions/ColorType"
          name:
            $ref: "#/definitions/Name"
          pets:
            type: array
            items:
              $ref: "#/definitions/Animal"

      Animal:
        description: The model of an animal.
        allOf:
          - $ref: "#/definitions/BaseModel"
        properties:
          color:
            $ref: "#/definitions/ColorType"
          owner:
            $ref: "#/definitions/Person"
          name:
            $ref: "#/definitions/Name"
          height:
            type: number
            format: float
            example: 1.2
            minimum: 0
          weight:
            type: number
            format: float
            example: 1.2
            minimum: 0

      Dog:
        description: The model of a dog.
        allOf:
          - $ref: "#/definitions/Animal"
        properties:
          barkDecibels:
            type: number
            format: float
            example: 1.2
            minimum: 0

      Dachschund:
        description: The model of a wiener dog.
        allOf:
          - $ref: "#/definitions/Dog"
        properties:
          apexPredator:
            type: boolean
            default: true
        required:
          - apexPredator

      Poodle:
        description: The model of a poodle.
        allOf:
          - $ref: "#/definitions/Dog"
        properties:
          dogShowsWon:
            type: number
            format: int64
            default: 0
            minimum: 0

      Cat:
        description: The model of a cat.
        allOf:
          - $ref: "#/definitions/Animal"
        properties:
          meowDecibels:
            type: number
            format: float
            example: 1.2
            minimum: 0

      Shark:
        description: The model of a shark.
        allOf:
          - $ref: "#/definitions/Animal"
        properties:
          peopleBitten:
            type: number
            format: int64
            default: 0
            minimum: 0
          friendly:
            type: boolean
            default: true

    properties:
      ColorType:
        $ref: "#/definitions/ColorType"
      BinaryFile:
        $ref: "#/definitions/BinaryFile"
      Name:
        $ref: "#/definitions/Name"
      BaseModel:
        $ref: "#/definitions/BaseModel"
      AdoptionTransaction:
        $ref: "#/definitions/AdoptionTransaction"
      Person:
        $ref: "#/definitions/Person"
      Animal:
        $ref: "#/definitions/Animal"
      Dog:
        $ref: "#/definitions/Dog"
      Dachschund:
        $ref: "#/definitions/Dachschund"
      Poodle:
        $ref: "#/definitions/Poodle"
      Cat:
        $ref: "#/definitions/Cat"
      Shark:
        $ref: "#/definitions/Shark"
```

### Domain Ontology: DAGs

Ubiquia can orchestrate Directed Acyclic Graphs—DAGs—provided a YAML definition that references a DDC. DAGs themselves can be defined within the "graphs" section of a DomainOntology.

```yaml
graphs:

  - name: pet-store-dag

    modelType: Graph

    description: I'm a Directed Acyclic Graph that shows off some Ubiquia capabilities

    # These are capabilities that the graph implements. These will eventually be used by Ubiquia's Executive Service.
    capabilities:
      - ImACapability!

    # These are a list of components that comprise this graph.
    components:

      - name: Pet-Store-Image-Classifier-Component

        # Setting a component as a template will instruct Ubiquia to not instantiate it as a 
        # Kubernetes pod, but instead to generate a "proxy" for it which will act on its behalf with 
        # dummy data.
        componentType: TEMPLATE
        modelType: Component
        description: This is a an example component that will classify an animal from an image.

        communicationServiceSettings:
          # Ensure the Communication Service exposes this component's endpoints  
          exposeViaCommService: true

        # This is the port that will be exposed for this component when it is deployed.
        port: 5000

        # Data pertaining to the image so that it can be deployed as pods/containers.
        image:
          registry: ubiquia
          repository: pet-store-image-classiffier
          tag: latest

        # An optional configmap to pass to the component.
        config:
          configMap:
            application.yml: |
              config_0:
                value: true
              config_1:
                value: demo-value
          configMountPath: /example/mountpath

        # This is a list of settings to use to override the baseline values should the graph be 
        # deployed with any "flags." Flags are passed to Ubiquia when deploying graphs via the GraphController 
        # RESTful interface.
        overrideSettings:

          # Any example override setting. It will override the baseline image values defined when the 
          # graph is deployed with a "exampleOverride" flag.
          - flag: exampleOverride
            key: image
            value:
              registry: exampleOverrideRegistry
              repository: exampleOverrideRepository
              tag: latest

          # Another example override setting. It will override the template component boolean when the graph
          # is deployed with the "demo" flag.
          - flag: demo
            key: componentType
            value: POD
          - flag: devops
            key: componentType
            value: POD

          # Yet another example to show that even nested configuration values can be overridden.
          - flag: exampleOverride
            key: config
            value:
              configMap:
                application.yml: |
                  config_0:
                    value: false
                  config_99:
                    value: demo-value
              configMountPath: /example/mountpath

      - name: Pet-Generator-Component

        componentType: TEMPLATE
        modelType: Component
        description: This is a an example component that will generate new Pets given a name.
        port: 8080
        image:
          registry: ubiquia
          repository: pet-store-data-transform
          tag: latest

        overrideSettings:
          - flag: demo
            key: componentType
            value: POD
          - flag: devops
            key: componentType
            value: POD

        node:
          

    # This is a list of nodes that should be deployed with the graph. Nodes may or may not be associated with components.
    nodes:

    - modelType: Node
      nodeType: PUSH
      name: Pet-Store-Image-Classifier-Node
      description: This is an example node.

      communicationServiceSettings:
        # Ensure the Communication Service exposes this nodes's endpoints  
        exposeViaCommService: true

      # This is the endpoint of the component that the node will interact with when it 
      # receives upstream traffic.
      endpoint: /upload-image

      # An example input schema that references a model of the domain ontology.
      inputSubSchemas:
        - modelName: BinaryFile

      # An example input schema that references a model of the domain ontology.
      outputSubSchema:
        modelName: ClassificationResult
      
      # Various configuration for the node, such as whether to persist payload traffic in the 
      # Ubiquia database for later diagnostics/analysis.
      nodeSettings:
        persistInputPayload: true
        persistOutputPayload: true
        validateOutputPayload: false
        validateInputPayload: false

      # Like components, nodes can have overridesettings.
      overrideSettings:
        - flag: demo
          key: nodeSettings
          value:
            persistInputPayload: false
            persistOutputPayload: false
            validateOutputPayload: true

    - modelType: Node
      nodeType: HIDDEN
      name: Pet-Generator-Node
      description: This is an example node.

      endpoint: /pet/store/create-pet

      inputSubSchemas:
        - modelName: ClassificationResult

      outputSubSchema:
        modelName: Animal
      nodeSettings:
        persistInputPayload: true
        persistOutputPayload: true
        validateOutputPayload: false
        validateInputPayload: false

      overrideSettings:
        - flag: demo
          key: nodeSettings
          value:
            persistInputPayload: true
            persistOutputPayload: true
            validateInputPayload: true
            validateOutputPayload: true
            stimulateInputPayload: true

    - modelType: Node
      nodeType: EGRESS
      name: Pet-Egress-Node
      description: This is an example node that POSTs incoming payloads to a belief state.

      endpoint: http://pets-belief-state-1-2-3:8080/ubiquia/Animal/add

      inputSubSchemas:
        - modelName: Animal

      egressSettings:
        httpOutputType: POST
        egressType: SYNCHRONOUS
        egressConcurrency: 1

      nodeSettings:
        persistInputPayload: true
        persistOutputPayload: true
        validateOutputPayload: false
        validateInputPayload: false

      overrideSettings:
        - flag: demo
          key: nodeSettings
          value:
            persistInputPayload: true
            persistOutputPayload: true
            validateInputPayload: true
            validateOutputPayload: true

    # This is how Ubiquia knows how to connect nodes together into a Directed Acyclic Graph. Conceptually, 
    # data flows from left-to-right. As nodes receive payloads, they will send them to their respective 
    # components (if they have them), package up the component response (if applicable), and send
    # the payload "downstream" (i.e., to any nodes "immediately on the right.")
    edges:
      - leftNodeName: Pet-Store-Image-Classifier-Node
        rightNodeNames:
          - Pet-Generator-Node
      - leftNodeName: Pet-Generator-Node
        rightNodeNames:
          - Pet-Egress-Node
```

#### DAGs: Flow

In Ubiquia, a "flow" is considered a discrete unit of work that starts when the first node of a DAG ingests data. All subsequent events within that flow will be considered to be a part of that parent flow — they will have the same `FlowId`. This flow can occur entirely within a single DAG, or across multiple DAGs across multiple Ubiquia Agents.

#### DAGs: Cardinality

DAGs can be deployed by individual Ubiquia Agents with "Cardinality" — or the ability to toggle on/off components/nodes of the DAG at deploy time. This allows multiple Ubiquia Agents to coordinate on a distributed DAG such that the agents can optimize execution of the DAG internals in a way best-suited to the compute resources available to those Ubiquia Agents.

The "Cardinality" is defined at deploy time, either as a part of the bootstrapping process (defined in Helm), or when a RESTful `GraphDeployment` payload is sent to Ubiquia instructing it to instantiate a new DAG. Importantly, these can be updated at runtime, allowing for Ubiquia agents to respond to computational requirements in real-time.

```yaml
ubiquia:
  agent:
    flowService:
      bootstrap:
        graph:
          deployments:
            - name: ubiquia-workbench
              version:
                major: 1
                minor: 0
                patch: 0
              cardinality:
                componentSettings:
                  - name: component-a
                    replicas: 0
                  - name: component-b
                    replicas: 1
                  - name: component-c
                    replicas: 2
```

If cardinality is not explicitly defined at deploy time, Ubiquia will default to `enabled = true` for every component and node.

## Belief States

Ubiquia allows clients to deploy fully RESTful, schema-validating, queryable **Belief State** services directly from a **Domain Data Contract (DDC)**. These services are automatically deployed into Kubernetes and support both ingestion and querying of relational data out of the box.

When configured to run against YugabyteDB, these belief states will be distributed and automatically sync with other belief states also connected to the same logical YugabyteDB cluster.

---

### Belief States: Querying Data

Belief State services expose dynamic query capabilities via the `/query/params/` endpoint. These endpoints validate all fields at runtime—invalid or unknown fields will result in HTTP errors.

Query endpoints are also **polymorphic**: querying a base model (e.g., `Animal`) may return instances of all subtypes (e.g., `Cat`, `Dachshund`).

---

#### Supported Operators

In addition to `equals`, the following operators are available directly in a GET request:

| Operator              | Syntax   |
|-----------------------|----------|
| less than or equal    | `<=`     |
| greater than or equal | `>=`     |
| equals null           | `=null`  |
| not null              | `=!null` |
| string match (like)   | `*=`     |

---

#### Example: Query by Parameters

Ubiquia's generated Belief States support querying data via URL GET endpoints:

```http
GET /ubiquia/belief-state-service/animal/query/params?page=0&size=25&sort-descending=true&created-at>=2022-08-30T21:00:00.000Z&name=!null
```

Response:

```json
{
  "content": [
    {
      "id": "daad6ddd-c060-4f52-ada3-f81e099948f2",
      "name": "Fang",
      "modelType": "Cat",
      "createdAt": "2022-11-02T14:26:45.244Z",
      "updatedAt": "2022-11-02T14:26:45.244Z",
      "whiskers": 10,
      "owner": {
        "name": "Mark",
        "id": "cbad6ddd-c060-4f52-ada3-f81e099948f2",
        "modelType": "Person"
      }
    },
    {
      "id": "baad6ddd-c060-4f52-ada3-f81e099948f2",
      "name": "Max",
      "modelType": "Dachshund",
      "createdAt": "2023-11-02T14:26:45.244Z",
      "updatedAt": "2023-11-02T14:26:45.244Z",
      "ApexPredator": true,
      "owner": {
        "name": "Sally",
        "id": "xyad6ddd-c060-4f52-ada3-f81e099948f2",
        "modelType": "Person"
      }
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 25
  },
  "totalElements": 2,
  "totalPages": 1,
  "numberOfElements": 2,
  "first": true,
  "last": true
}
```

#### Example: Relational Queries

Ubiquia Belief States support querying for relational data out of the box:

```http
GET /ubiquia/belief-state-service/animal/query/params?page=0&size=25&sort-descending=true&created-at>=2022-08-30T21:00:00.000Z&owner.name=Mark
```

Response:

```json
{
  "content": [
    {
      "id": "daad6ddd-c060-4f52-ada3-f81e099948f2",
      "name": "Fang",
      "modelType": "Cat",
      "createdAt": "2022-11-02T14:26:45.244Z",
      "updatedAt": "2022-11-02T14:26:45.244Z",
      "whiskers": 10,
      "owner": {
        "name": "Mark",
        "id": "cbad6ddd-c060-4f52-ada3-f81e099948f2",
        "modelType": "Person",
        "createdAt": "2022-11-02T14:26:45.244Z",
        "updatedAt": "2022-11-02T14:26:45.244Z"
      }
    }
  ],
  "pageable": {
    "sort": {
      "empty": true,
      "sorted": false,
      "unsorted": true
    },
    "offset": 0,
    "pageSize": 25,
    "pageNumber": 0,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "totalElements": 1,
  "totalPages": 1,
  "size": 25,
  "number": 0,
  "sort": {
    "empty": true,
    "sorted": false,
    "unsorted": true
  },
  "first": true,
  "numberOfElements": 1,
  "empty": false
}
```

### Querying Data: Multiselect

Belief States support multiselect endpoints that allow clients to define only the fields that they would like back from a RESTful query for performance-minded queries. These endpoints support pagination, sorting, and filtering.

Multiselect syntax:

```http
GET /ubiquia/belief-state-service/animal/query/multiselect/params?page=0&size=1&multiselect-fields=createdAt,updatedAt
```

Multiselect response:

```json
{
  "content": [
    [
      "2024-04-26T19:54:30.158154Z",
      "2024-04-26T19:54:31.103444Z"
    ]
  ],
  "number": 0,
  "size": 1,
  "totalElements": 5,
  "pageable": {
    "pageNumber": 0,
    "pageSize": 1,
    "sort": {
      "empty": true,
      "sorted": false,
      "unsorted": true
    },
    "offset": 0,
    "unpaged": false,
    "paged": true
  },
  "last": false,
  "totalPages": 5,
  "sort": {
    "empty": true,
    "sorted": false,
    "unsorted": true
  },
  "first": true,
  "numberOfElements": 1,
  "empty": false
}
```

## Datastores

Ubiquia supports different datastore configurations. Primarily, this boils down to a relational datastore choice between a distributed YugabyteDB SQL setting or an embedded H2 SQL setting, and whether or not to enable a [MinIO](https://www.min.io) object storage layer. Both Ubiquia's Core Flow Service and generated Belief States will use the configured relational datastore.

Moreover, generated Belief States will automatically connect against the internal MinIO instance should MinIO be enabled. Thus, clients can upload binaries (or anything) to MinIO for later retrieval. ***Importantly, the metadata of these objects will be housed in the relational database.*** This last point is important for when multiple Ubiquia Agents are running against a distributed YugabyteDB cluster and may need specific artifacts available only locally to one agent. In this specific case, Ubiquia Agents will be aware of artifacts available locally to other agents and be able to retrieve them through the communication service.

### Datastores: Configuration

Configuration of Ubiquia datastores is defined via Helm per any values configuration per the below:

```yaml
ubiquia:
  agent:
    database:
      h2:
        enabled: false
      yugabyte:
        enabled: true
    storage:
      minio:
        enabled: true
```

## For Devs

This section contains a handful of useful commands, topics, concepts, and other notes for developers using the Ubiquia framework.

### For Devs: Building Ubiquia

The entire Ubiquia project can be built by invoking Gradle. This will task each subproject to build.

```bash
./gradlew clean build
```

### For Devs: Building Subprojects

The subprojects of Ubiquia can be built by invoking specific subprojects via Gradle.

Generic example:

```bash
./gradlew :<folder>:<folder>:<subproject>:<command>
```

Concrete example:

```bash
./gradlew :services:core:java:core-flow-service:build
```

### For Devs: Simulation CLI (`util-simulation-service`)

The `util-simulation-service` is a CLI utility for exercising multi-agent flow deployments against a live Ubiquia environment.

#### Installation

From the repo root:

```bash
uv tool install -e services/util/python/util-simulation-service
```

#### Usage

```bash
# Top-level help
util-simulation-service --help

# Simulation subcommand help
util-simulation-service simulation --help

# Run a simulation
util-simulation-service simulation run --input-file <path-to-simulation.json>
```

#### Simulation Input File

The `--input-file` argument points to a JSON file describing the simulation. A ready-to-use example is included in the service:

```bash
util-simulation-service simulation run \
  --input-file services/util/python/util-simulation-service/resources/simulation.json
```

The input file schema:

```json
{
  "name": "dry-run",
  "agents": [
    {"name": "agent-0", "mode": "microweight"}
  ],
  "events": [],
  "networks": [],
  "speed": 1.0
}
```

| Field | Description |
|-------|-------------|
| `name` | Identifier for this simulation run |
| `agents` | List of agents to set up; each has a `name` and `mode` (`microweight` or `kind`) |
| `events` | Simulation events to replay against the deployment |
| `networks` | Docker network configuration for the simulation |
| `speed` | Playback speed multiplier (`1.0` = real time) |

The CLI must be run from within the git repository (it uses `git rev-parse --show-toplevel` to locate the repo root).

---

## Who Is This For?

- **ML engineers** who want LLM components with schema validation  
- **Backend developers** who want to design systems with declarative intent and runtime validation  
- **Systems engineers** building distributed or autonomous orchestration platforms  
- **Scientists & researchers** modeling intelligent ecosystems or recursive planners  
- **DoD/IC technologists** looking to modernize simulation, orchestration, or planning infrastructure  

---

## Advanced Topics

### Advanced Topics: Adapter Backpressure

Ubiquia nodes leverage an inbox/outbox mechanism to ensure that they can pop messages off of the database queue. This is especially important when the database is distributed. Adapters provide a backpressure endpoint that can be used to show how many records are in this queue, and the rate at which this queue is growing (or shrinking).

The nodes themselves provide this information; it is up to external entities to act upon that information. As of now, the plan is to have the Ubiquia Executive Command and/or Kubernetes autoscaling scale up nodes as necessary to alleviate this backpressure should a queue grow.

### Advanced Topics: PostStartExecCommands

It is sometimes necessary for components deployed within Ubiquia DAGs to have some form of post-start hook. Ubiquia leverages the Kubernetes `PostStartExecCommand` by allowing developers to configure an array of arguments that the component should invoke after it has come alive as a Kubernetes pod.

```yaml
name: Workbench-LLM

componentType: POD
modelType: Component
description: An LLM for our workbench.

image:
  registry: ollama
  repository: ollama
  tag: 0.11.5

postStartExecCommands:
  - /bin/sh
  - -c
  - |
    set -eu
    echo "Waiting for Ollama..."
    i=0
    while [ "$i" -lt 360 ]; do
      if ollama list >/dev/null 2>&1; then
        echo "Daemon up. Pulling model..."
        ollama pull llama3.2
        exit 0
      fi
      i=$((i+1))
      sleep 1
    done
    echo "Ollama not ready after 360s" >&2
    exit 1
```

## Glossary

| Term | Definition |
|------|------------|
| **DDC (Domain Data Contract)** | A JSON Schema-based contract that defines the types of messages components and nodes can send or receive. Enforces runtime validation of component I/O. |
| **Adapter** | A software component that connects nodes in a DAG and defines how messages are transported or transformed (e.g., `publish`, `merge`, `poll`). |
| **Component** | A stateful microservice deployed as part of a DAG, capable of sending, receiving, and acting on messages according to DDCs. |
| **Componentless Adapter** | A node in a DAG that performs flow control (e.g., routing, polling, merging) but does not host a component implementation. |
| **Belief State** | A shared, distributed, and SQL-backed representation of the system’s current knowledge. Nodes can read from and write to it, supporting coordination and memory across the system. |
| **Belief State Generator** | A codegen service that transforms DDCs into typed Java classes and Spring Boot REST services, enabling components to interact with the belief state in a schema-safe way. |
| **Communication Service** | A reverse proxy and routing gateway that dynamically exposes core services and component/nodes based on DAG configuration. |
| **DAO (Data Access Object)** | A component that abstracts and encapsulates database interactions, commonly used to query or persist belief state entities. |
| **DAG (Directed Acyclic Graph)** | A directed graph with no cycles, used to define component topologies and message flow in Ubiquia. DAGs are authored in YAML and compiled into orchestrated services. |
| **DAG Manifest** | A YAML configuration file that declares how a DAG and its components/nodes should be deployed, configured, and interconnected. |
| **DTO (Data Transfer Object)** | A simple object used to encapsulate data transferred between layers or services in Ubiquia. Used heavily in REST APIs. |
| **Flow Service** | A core Ubiquia microservice responsible for materializing DAGs into running components and nodes. Manages lifecycle, registration, and event querying. |
| **MAO (Multi-Agent Orchestration)** | The process of managing and coordinating interactions among components and nodes in a MAS. Ubiquia handles MAO through DAG deployment and node coordination. |
| **MAS (Multi-Agent System)** | A system composed of multiple intelligent nodes that interact or work together to perform tasks or solve problems. Ubiquia provides runtime infrastructure for these systems. |
| **Schema Registry** | A repository of JSON Schemas (DDCs) that define I/O contracts for components and services. Used for validation, code generation, and schema evolution. |

---

## Contributors

* __Jeremy Case__: jeremycase@odysseyconsult.com

## License

Apache 2.0 — Open source, extensible, and ready for real-world deployment.
