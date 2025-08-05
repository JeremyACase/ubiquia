
# 🧠 `belief-state-generator-service`

> **A schema-aware code generation and compilation engine for belief state APIs in Ubiquia.**

The `belief-state-generator-service` is a **code generation microservice** in Ubiquia. It transforms declaratively defined schemas — along with optional inheritance rules, decorators, and DAG metadata — into **runtime-compiled Java services** that expose **typed RESTful belief state endpoints**.

This service automates the lifecycle of belief state logic — from raw schema to live, type-safe API — enabling distributed components to share structured knowledge across DAGs without boilerplate or manual coding.

---

## 🚀 Responsibilities

- 🧬 Transform component communication schemas into Java model classes and services  
- 🔧 Compile and register generated belief-state services at runtime  
- 🪄 Apply decorators (e.g., inheritance, metadata injection) before codegen  
- 🔁 Support dynamic re-generation and recompilation of belief-state APIs  
- 📦 Package generated artifacts with Ubiquia runtime via Spring Boot  
- 📦 Includes a custom `build.gradle` setup that pre-fetches and resolves belief state dependencies before runtime compilation  
- 🧩 Interface with the schema registry, flow-service, and Kubernetes runtime

---

## 📦 Features

- **Schema-Aware Model Generation**  
  Leverages JSON Schema definitions to create structured, typed models for belief state interaction

- **Runtime Compilation Engine**  
  Uses Java compilation APIs to produce deployable microservices during runtime

- **Decorator Pipeline**  
  Optional preprocessors (e.g., inheritance, ACL generators, custom metadata) inject domain-specific logic into the generation process

- **Belief State Builders**  
  Builders provide clean generation of component classes and deployment logic

- **Spring Boot Integration**  
  All generated classes and endpoints are auto-wired into the running Spring context

---

## 🛠️ Building the Service

```bash
./gradlew clean :core:service:belief-state-generator-service:build
```

### 🏃 Running the Service (Locally)

```bash
./gradlew :core:service:belief-state-generator-service:bootRun
```

---

## 🧰 Project Structure

```text
src/main/java/org/ubiquia/core/belief/state/generator
├── controller/            # REST endpoints for triggering generation or introspection
├── service/
│   ├── builder/           # Codegen builders for names and deployments
│   ├── compile/           # Java source compilation engine
│   ├── generator/         # Main generation engine and cleanup processors
│   ├── decorator/         # Decorators for schema injection, inheritance, ACL typing
│   ├── k8s/               # Kubernetes operators 
│   ├── logic/             # Internal logic for this microservice 
│   └── mapper/            # Mappers that transform JSON Schema into OpenAPI yaml 
├── config/                # Wiring and service config for dependencies
├── Application.java       # Spring Boot entrypoint
├── Config.java            # DI configuration and service registration
├── resources/             # Resources to be packaged with the application 
├── build.gradle           # The Gradle build for the Flow Service.
├── Dockerfile             # The Dockerfile that defines how this is containerized in devops pipelines 
└──
```

---

## 📚 Exploring the API (Swagger UI)

Once running locally, explore the generator API at:

```
http://localhost:8080/swagger-ui/index.html
```

Use the Swagger UI to:
- Trigger generation from new schemas
- Recompile belief state APIs
- Inspect metadata, errors, or decorated schema state

---

## 🧬 Relationship to Ubiquia

This service is part of Ubiquia's internal infrastructure for runtime type safety and schema-first design. It works closely with:

- `flow-service`: to map DAGs to belief states  
- `schema-registry-service`: to fetch and register JSON schemas  
- `belief-state-service`: to serve compiled belief states across the DAG

---

## 📜 License

This module is part of Ubiquia and licensed under [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0).
