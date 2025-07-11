
# 🗃️ `dao`

> **A lightweight, schema-aware query construction engine for dynamic persistence in Ubiquia.**

The `dao` module provides the core building blocks for type-safe, runtime-driven query construction in Ubiquia. It offers declarative tools for building **dynamic predicates**, deriving **entity and class metadata**, and mapping **query filters and parameters** — all in support of runtime-compiled belief state services and adaptive components.

---

## 🚀 Responsibilities

- 🧱 Represent structured query constraints via `EntityDao`, `FilterDao`, and `ParameterDao`  
- 🧠 Generate typed query predicates dynamically from nested schema structures  
- 🔍 Derive JPA entities, fields, and embeddables via reflection and metadata discovery  
- 🛠 Construct complex `WHERE` logic using composable predicate builders  
- 🧩 Act as the foundation for query translation in belief-state libraries and generator services

---

## 📦 Features

- **EntityDao and FilterDao Abstractions**  
  Capture declarative representations of query filters, logical combinations, and search conditions

- **Predicate Builders**  
  Convert structured filters into executable JPA Criteria logic, with support for both nested and flat object graphs

- **Metadata Derivation**  
  Tools like `EntityDeriver` and `EmbeddableDeriver` inspect runtime model structure to support belief state compilation

- **Composable Logic**  
  Each component is modular, testable, and designed to plug into Ubiquia’s runtime generation ecosystem

---

## 🛠️ Building the Module

```bash
./gradlew clean :common:library:dao:build
```

---

## 🧰 Project Structure

```text
src/main/java/org/ubiquia/common/library/dao
├── component/             # Declarative representations: EntityDao, FilterDao, etc.
├── service/
│   ├── builder/           # Predicate builders for Criteria API logic
│   ├── logic/             # Derivers for reflection-based schema understanding
```

---

## 🔗 Usage Context

This module is heavily used by:

- `belief-state-libraries`: to parse filter expressions and support dynamic REST API queries  
- `belief-state-generator-service`: during schema-to-service compilation to bind queries to models  
- `flow-service`: indirectly via compiled components that expose queryable endpoints as well as by Flow Service controllers

---

## 📜 License

This module is part of Ubiquia and licensed under [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0).
