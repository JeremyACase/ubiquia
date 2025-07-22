# Ubiquia

> **Ubiquia is a distributed, schema-driven multi-agent orchestration system. Define agent DAGs declaratively, and Ubiquia automatically deploys and wires them into a live Kubernetes ecosystem—with optional belief state services for distributed, persistent, queryable shared context. All data is governed by schemas, ensuring reliable structure across all agents and components--including LLMs.**

---

## 🌟 Why Ubiquia?

Modern agent-based systems often rely on brittle glue code: ad-hoc APIs, hand-rolled orchestration, and inconsistent state handling. **Ubiquia replaces that mess with clean, declarative infrastructure**:

- 🔁 **Composable DAGs**  
  Describe workflows as YAML-based DAGs. Ubiquia spins up agents, components, adapters, belief states, and communication services on demand.

- 📦 **Schema-to-Belief Pipelines**  
  Provide an Agent Communication Language (i.e., JSON Schema), and Ubiquia will deploy a **RESTful Belief State service** with:  
  - Distributed persistence  
  - A fully-typed REST API generated from your schema  
  - A relational back-end  
  - An expressive, intuitive query API with paginated results
  - Real-time ingestion of normalized relational data

- 🧬 **Schema-Driven Stability**  
  Prevent long-term degeneration in multi-agent systems. Every I/O channel is bound to a contract defined in JSON Schema, keeping agents—including LLMs—grounded in clean, structured, machine-validated data.

- ⚙️ **Kubernetes-Native by Design**  
  Helm-first deployments, Prometheus & Micrometer observability baked in. Built for production from day one.

- 🌍 **Cross-Cluster DAG Communication**  
  DAGs can span **multiple Kubernetes clusters**, enabling components to operate across physical and cloud boundaries:  
  - **Resilient Compute**: workloads can route around degraded or partitioned clusters  
  - **Topology-Aware Execution**: deploy agents near data, users, or available compute zones

---

Unlike most MAS (Multi-Agent System) frameworks, **Ubiquia is designed from the ground up for real-world, production-grade integration**. It combines formal schema enforcement with robust deployment tooling, dynamic service generation, and cross-cluster communication—bridging the gap between research prototypes and hardened operational systems.

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
* [Agent Communication Language](#agent-communication-language)
  * [Agent Communication Language: Defining ACLs](#agent-communication-language-defining-acls)
* [DAGs](#dags)
* [Belief States](#belief-states)
  * [Belief States: Querying Data](#belief-states-querying-data)
* [For Devs](#for-devs)
  * [For Devs: Building Ubiquia](#for-devs-building-ubiquia)
  * [For Devs: Building Subprojects](#for-devs-building-subprojects)
* [Contributors](#contributors)
* [Who Is This For?](#who-is-this-for)
* [Glossary](#glossary)
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

After invoking the script and a successful installation, Helm will output to console how to interface with the newly-installed Ubiquia component.

### Quickstart: Deleting Ubiquia Cluster
If you ran the above script to install Ubiquia into KIND and want a completely fresh start, you can delete the KIND Kubernetes cluster

```bash
$ kind delete clusters ubiquia-component-0
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
Ubiquia is a modular multi-component orchestration platform designed for scalable, belief-driven AI systems running on Kubernetes.

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

## Agent Communication Language
Ubiquia revolves around the the idea that schemas are necessary to prevent Multi-Agent systems from collapsing under their own weight. To battle entropy at scale, Ubiquia requires an Agent Communication Language (ACL) before orchestrating any DAGs or Belief States. ACLs are themselves really minimal metadata around the [JSON Schema](https://json-schema.org/) specification.

### Agent Communication Language: Defining ACLs
Here's an example ACL:

```json
{
  "domain": "pets",
  "version": {
    "major": 1,
    "minor": 2,
    "patch": 3
  },
  "modelType": "AgentCommunicationLanguage",
  "jsonSchema": {
    "type": "object",
    "definitions": {
      "ColorType": {
        "type": "string",
        "enum": [
          "BLUE",
          "GREEN",
          "BROWN",
          "BLACK",
          "WHITE",
          "GRAY"
        ]
      },
      "Name": {
        "description": "A model with some name information.",
        "type": "object",
        "properties": {
          "firstName": {
            "type": "string"
          },
          "lastName": {
            "type": "string"
          }
        },
        "required": [
          "firstName"
        ]
      },
      "BaseModel": {
        "description": "A base model.",
        "type": "object",
        "properties": {
          "info": {
            "type": "string",
            "maxLength": 100,
            "default": "Just some info about this model."
          }
        }
      },
      "AdoptionTransaction": {
        "description": "A transaction of an adoption.",
        "type": "object",
        "allOf": [
          {
            "$ref": "#/definitions/BaseModel"
          }
        ],
        "properties": {
          "owner": {
            "$ref": "#/definitions/Person"
          },
          "pet": {
            "$ref": "#/definitions/Animal"
          }
        }
      },
      "ClassificationResult": {
        "description": "The result of our ML model making a classification inference.",
        "type": "object",
        "allOf": [
          {
            "$ref": "#/definitions/BaseModel"
          }
        ],
        "properties": {
          "class": {
            "type": "string",
            "description": "The class label predicted by the model."
          },
          "confidence": {
            "type": "number",
            "format": "float",
            "minimum": 0.0,
            "maximum": 1.0,
            "description": "The confidence score (between 0 and 1) for the predicted class."
          }
        },
        "required": ["class", "confidence"]
      },
      "Person": {
        "description": "A model of a person.",
        "type": "object",
        "allOf": [
          {
            "$ref": "#/definitions/BaseModel"
          }
        ],
        "properties": {
          "hairColor": {
            "$ref": "#/definitions/ColorType"
          },
          "name": {
            "$ref": "#/definitions/Name"
          },
          "pets": {
            "type": "array",
            "items": {
              "$ref": "#/definitions/Animal"
            }
          }
        }
      },
      "Animal": {
        "description": "The model of an animal.",
        "allOf": [
          {
            "$ref": "#/definitions/BaseModel"
          }
        ],
        "properties": {
          "color": {
            "$ref": "#/definitions/ColorType"
          },
          "owner": {
            "$ref": "#/definitions/Person"
          },
          "name": {
            "$ref": "#/definitions/Name"
          },
          "height": {
            "type": "number",
            "format": "float",
            "example": 1.2,
            "minimum": 0
          },
          "weight": {
            "type": "number",
            "format": "float",
            "example": 1.2,
            "minimum": 0
          }
        }
      },
      "Dog": {
        "description": "The model of a dog.",
        "allOf": [
          {
            "$ref": "#/definitions/Animal"
          }
        ],
        "properties": {
          "barkDecibels": {
            "type": "number",
            "format": "float",
            "example": 1.2,
            "minimum": 0
          }
        }
      },
      "Dachschund": {
        "description": "The model of a wiener dog.",
        "allOf": [
          {
            "$ref": "#/definitions/Dog"
          }
        ],
        "properties": {
          "apexPredator": {
            "type": "boolean",
            "default": true
          }
        },
        "required": [
          "apexPredator"
        ]
      },
      "Poodle": {
        "description": "The model of a poodle.",
        "allOf": [
          {
            "$ref": "#/definitions/Dog"
          }
        ],
        "properties": {
          "dogShowsWon": {
            "type": "number",
            "format": "int64",
            "default": 0,
            "minimum": 0
          }
        }
      },
      "Cat": {
        "description": "The model of a cat.",
        "allOf": [
          {
            "$ref": "#/definitions/Animal"
          }
        ],
        "properties": {
          "meowDecibels": {
            "type": "number",
            "format": "float",
            "example": 1.2,
            "minimum": 0
          }
        }
      },
      "Shark": {
        "description": "The model of a shark.",
        "allOf": [
          {
            "$ref": "#/definitions/Animal"
          }
        ],
        "properties": {
          "peopleBitten": {
            "type": "number",
            "format": "int64",
            "default": 0,
            "minimum": 0
          },
          "friendly": {
            "type": "boolean",
            "default": true
          }
        }
      }
    },
    "properties": {
      "ColorType": {
        "$ref": "#/definitions/ColorType"
      },
      "Name": {
        "$ref": "#/definitions/Name"
      },
      "BaseModel": {
        "$ref": "#/definitions/BaseModel"
      },
      "AdoptionTransaction": {
        "$ref": "#/definitions/AdoptionTransaction"
      },
      "Person": {
        "$ref": "#/definitions/Person"
      },
      "Animal": {
        "$ref": "#/definitions/Animal"
      },
      "Dog": {
        "$ref": "#/definitions/Dog"
      },
      "Dachschund": {
        "$ref": "#/definitions/Dachschund"
      },
      "Poodle": {
        "$ref": "#/definitions/Poodle"
      },
      "Cat": {
        "$ref": "#/definitions/Cat"
      },
      "Shark": {
        "$ref": "#/definitions/Shark"
      }
    }
  }
}
```

## DAGs
Ubiquia can orchestrate Directed Acyclic Graphs--DAGs--provided a yaml definition that references an ACL.

```yaml
graphName: Pets
modelType: Graph
author: Jeremy Case
description: The "Hello World" of Directed Acyclic Graphs (DAG's) in Ubiquia, but with Pets!

# Graphs require semantic versions
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

# These are capabilities that the graph implements. These will eventually be used by Ubiquia's Executive Service.
capabilities:
  - ImACapability!

# This is the "Agent Communication Language" that this graph is associated with.
agentCommunicationLanguage:
  name: pets
  version:
    major: 1
    minor: 2
    patch: 3

# These are a list of components that comprise this graph.
components:

  - componenttName: Pet-Store-Image-Classifier-Component

    # Setting a component as a template will instruct Ubiquia to not instantiate it as a 
    # Kubernetes pod, but instead to generate a "proxy" for it which will act on its behalf with 
    # dummy data.
    componentName: TEMPLATE
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

    # This is an "adapter" for the component; Ubiquia will use this to manage DAG network 
    # traffic to/from the component.
    adapter:
      modelType: Adapter
      adapterType: PUSH
      adapterName: Pet-Store-Image-Classifier-Adapter
      description: This is an example adapter.

      communicationServiceSettings:
        # Ensure the Communication Service exposes this adapter's endpoints  
        exposeViaCommService: true

      # This is the endpoint of the component that the adapter will interact with when it 
      # receives upstream traffic.
      endpoint: /upload-image

      # An example input schema that references a model of the domain ontology.
      inputSubSchemas:
        - modelName: BinaryFile

      # An example input schema that references a model of the domain ontology.
      outputSubSchema:
        modelName: ClassificationResult
      
      # Various configuration for the adapter, such as whether to persist payload traffic in the 
      # Ubiquia database for later diagnostics/analysis.
      adapterSettings:
        persistInputPayload: true
        persistOutputPayload: true
        validateOutputPayload: false
        validateInputPayload: false

      # Like components, Adapters can have overridesettings.
      overrideSettings:
        - flag: demo
          key: adapterSettings
          value:
            persistInputPayload: false
            persistOutputPayload: false
            validateOutputPayload: true

  - componentName: Pet-Generator-Component

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

    adapter:
      modelType: Adapter
      adapterType: HIDDEN
      adapterName: Pet-Generator-Adapter
      description: This is an example adapter.

      communicationServiceSettings:
        exposeViaCommService: true

      endpoint: /pet/store/create-pet

      inputSubSchemas:
        - modelName: ClassificationResult

      outputSubSchema:
        modelName: Animal
      adapterSettings:
        persistInputPayload: true
        persistOutputPayload: true
        validateOutputPayload: false
        validateInputPayload: false

      overrideSettings:
        - flag: demo
          key: adapterSettings
          value:
            persistInputPayload: true
            persistOutputPayload: true
            validateInputPayload: true
            validateOutputPayload: true
            stimulateInputPayload: true

# This is a list of adapters that should be deployed with the graph that are not to be associated
# with components. These are typically useful as ingress/egress components to a DAG.
componentlessAdapters:

  - modelType: Adapter
    adapterType: EGRESS
    adapterName: Pet-Egress-Adapter
    description: This is an example adapter that POSTs incoming payloads to a belief state.

    endpoint: http://pets-belief-state-1-2-3:8080/ubiquia/Animal/add

    inputSubSchemas:
      - modelName: Animal

    egressSettings:
      httpOutputType: POST
      egressType: SYNCHRONOUS
      egressConcurrency: 1

    adapterSettings:
      persistInputPayload: true
      persistOutputPayload: true
      validateOutputPayload: false
      validateInputPayload: false

    overrideSettings:
      - flag: demo
        key: adapterSettings
        value:
          persistInputPayload: true
          persistOutputPayload: true
          validateInputPayload: true
          validateOutputPayload: true

# This is how Ubiquia knows how to connect adapters together into a Directed Acyclic Graph. Conceptually, 
# data flows from left-to-right. As adapters receive payloads, they will send them to their respective 
# components (if they have them), package up the component response (if applicable), and send
# the payload "downstream" (i.e., to any adapters "immediately on the right.")
edges:
  - leftAdapterName: Pet-Store-Image-Classifier-Adapter
    rightAdapterNames:
      - Pet-Generator-Adapter
  - leftAdapterName: Pet-Generator-Adapter
    rightAdapterNames:
      - Pet-Egress-Adapter

```

## Belief States

Ubiquia allows clients to deploy fully RESTful, schema-validating, queryable **Belief State** services directly from an Agent Communication Language (ACL — really just JSON Schema). These services are automatically deployed into Kubernetes and support both ingestion and querying of relational data out of the box.

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

response:

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
Ubiquia Belief States support querying for relational data, out of the box:

```http
GET /ubiquia/belief-state-service/animal/query/params?page=0&size=25&sort-descending=true&created-at>=2022-08-30T21:00:00.000Z&owner.name=Mark
```

response:

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

Belief States support "multiselect" endpoints that allow clients to define ONLY the fields that they would like back from a RESTful query for performance-minded queries. These endpoints support pagination, sorting, and the sort. 

Multiselect syntax:

```http
GET /ubiquia/belief-state-service/animal/query/multiselect/params?page=0&size=1&multiselect-fields=createdAt,updatedAt
```

Multiselect Response:

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

- **ML engineers** who want LLM components with schema validation  
- **Backend developers** who want to design systems with declarative intent and runtime validation  
- **Systems engineers** building distributed or autonomous orchestration platforms  
- **Scientists & researchers** modeling intelligent ecosystems or recursive planners  
- **DoD/IC technologists** looking to modernize simulation, orchestration, or planning infrastructure

---

## Glossary

| Term | Definition |
|------|------------|
| **ACL (Agent Communication Language)** | A JSON Schema-based contract that defines the types of messages components can send or receive. Enforces runtime validation of component I/O. |
| **Adapter** | A software component that connects nodes in a DAG and defines how messages are transported or transformed (e.g., `publish`, `merge`, `poll`). |
| **Component** | A stateful microservice deployed as part of a DAG, capable of sending, receiving, and acting on messages according to ACLs. |
| **Componentless Adapter** | An adapter node in a DAG that performs flow control (e.g., routing, polling, merging) but does not host an component implementation. |
| **Belief State** | A shared, distributed, and SQL-backed representation of the system’s current knowledge. Agents can read from and write to it, supporting coordination and memory across the system. |
| **Belief State Generator** | A codegen service that transforms ACLs into typed Java classes and Spring Boot REST services, enabling components to interact with the belief state in a schema-safe way. |
| **Communication Service** | A reverse proxy and routing gateway that dynamically exposes core services and component/adapters based on DAG configuration. |
| **DAO (Data Access Object)** | A component that abstracts and encapsulates database interactions, commonly used to query or persist belief state entities. |
| **DAG (Directed Acyclic Graph)** | A directed graph with no cycles, used to define component topologies and message flow in Ubiquia. DAGs are authored in YAML and compiled into orchestrated services. |
| **DAG Manifest** | A YAML configuration file that declares how a DAG and its components/adapters should be deployed, configured, and interconnected. |
| **DTO (Data Transfer Object)** | A simple object used to encapsulate data transferred between layers or services in Ubiquia. Used heavily in REST APIs. |
| **Flow Service** | A core Ubiquia microservice responsible for materializing DAGs into running components and adapters. Manages lifecycle, registration, and event querying. |
| **MAO (Multi-Agent Orchestration)** | The process of managing and coordinating interactions among components in a MAS. Ubiquia handles MAO through DAG deployment and adapter coordination. |
| **MAS (Multi-Agent System)** | A system composed of multiple intelligent components that interact or work together to perform tasks or solve problems. Ubiquia provides runtime infrastructure for these systems. |
| **Schema Registry** | A repository of JSON Schemas (ACLs) that define I/O contracts for components and services. Used for validation, code generation, and schema evolution. |
---

## Contributors
* __Jeremy Case__: jeremycase@odysseyconsult.com

## License

Apache 2.0 — Open source, extensible, and ready for real-world deployment.