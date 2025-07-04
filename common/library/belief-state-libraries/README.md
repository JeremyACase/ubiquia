
# 🧱 `belief-state-libraries`

> **Shared library components for typed, schema-driven belief state services in Ubiquia.**

The `belief-state-libraries` module provides foundational building blocks for belief-state services in Ubiquia. It contains reusable components for **typed data handling**, **RESTful controller scaffolding**, **DTO transformation**, **JPA persistence**, and **entity relationship management** — all optimized for schema-generated, runtime-compiled agent belief systems.

---

## 🚀 Responsibilities

- 🧩 Define reusable controller, service, and repository interfaces for belief state services  
- 🔄 Provide dynamic mappers for transforming between entities and I/O DTOs  
- 🧬 Support nested entity relationships, association models, and JPA orchestration  
- ⚙️ Enable telemetry tagging, response pagination, and relationship auto-wiring  
- 🛠 Supply common utilities and builders used during runtime code generation  
- 🧠 Abstract and enforce I/O contracts via interfaces such as `InterfaceModelController` and `EntityRepository`

---

## 📦 Features

- **Abstract Controller Logic**  
  Shared base classes like `AbstractAclModelController` streamline CRUD and ACL operations

- **Entity Relationship Builder**  
  Dynamically links model fields with parent/child/collection relationships, supporting runtime graph traversal

- **DTO Mapping & Pagination**  
  Builders like `DtoPageBuilder` handle transformation and pagination of complex nested data structures

- **JPA Repository Interface**  
  Provides contract-based persistence patterns via `EntityRepository`

- **Telemetry Integration**  
  Micrometer support via builders and commands for standardized observability across agents

---

## 🛠️ Building the Library

```bash
./gradlew clean :common:library:belief-state-libraries:build
```

---

## 🧰 Project Structure

```text
src/main/java/org/ubiquia/common/library/belief/state/libraries
├── controller/            # Abstract REST controller scaffolding (e.g., ACL-based CRUD)
├── interfaces/            # Common interface definitions for controllers, services, and DTO mappers
├── model/association/     # Nested relationship models like ParentAssociation and ChildAssociation
├── repository/            # Contract-based JPA persistence
├── service/
│   ├── builder/           # Builders for relationships, DTOs, telemetry
│   ├── finder/            # Service discovery and linkage tools
│   ├── command/           # Commands related to telemetry and tagging
```

---

## 🔗 Usage Context

This library is used by:
- `belief-states`: to serve compiled belief state APIs
- `belief-state-generator-service`: during generation of belief states
- `flow-service`: indirectly, when DAG nodes interact with structured belief memory

It enables runtime systems to reason about relationships between entities, enforce schema validation, and generate type-safe, REST-exposed knowledge graphs.

---

## 📜 License

This module is part of Ubiquia and licensed under [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0).
