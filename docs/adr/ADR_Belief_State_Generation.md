# Architecture Decision Record: Ye of Little Belief State

## Decision
Ubiquia will support the ability to generate RESTful, statically-typed, JSON-validating, distributed "Belief States" and deploy them into a Kubernetes environment from only a single ***Agent Communication Language*** input.

## Status

### [1.0.0] - 2025-07-01
- Accepted.

## Summary

### Pros
- Single Source of Truth (SST) for any given "domain"
- Versioned Agent Communication Languages for a domain
- Push-button generation of full-up RESTful servers with schema-native validation and guardrails
- Automated code and model generation
- Belief State transparency and auditability
- Cross-language interoperability with RESTful Belief States (language-agnostic by design)

### Cons
- Belief States are coupled to Agent Communication Language versions
- Data evolution in a distributed database (e.g., Yugabyte) still requires something like Liquibase
- World's nastiest Java code that abuses reflection, Spring, Hibernate, compilers, OpenAPI, and JPA all in equal measure
- Black Magic

### Alternatives
- Another flavor of Black Magic

## Context

> "All that? From a single JSON Schema input? A strongly-typed, dynamically-queryable, completely usable RESTful server fully integrated with any SQL database, with JSON validation, a Swagger UI, and even Minio for object storage? And deployed automatically into a Kubernetes environment as a pod behind a Kubernetes service? DTO's, entities, servers, and SQL, all in harmony? And transparently? Only a madman would attempt such a thing." – Comment made to Ubiquia author, who is a madman.

It is self-evident to the author that any Multi-Agent System (MAS) will collapse under its own weight unless the agents communicate using a robust schema. This is true amongst any two agents as they communicate over a network, but it is also true of how any number of agents perceive their world and share that knowledge (i.e., the agents' belief state). Indeed, this is a [major and ongoing area of study in MAS](https://www.ietf.org/archive/id/draft-narajala-ans-00.html). The author's experience, however, is in distributed software engineering and not academic MAS. Thus, the author believes that [sufficiently robust schemas already exist](https://json-schema.org/). What is missing, however, is ensuring that a multi-agent _system_ is cohesive within that schema — and this is where the author feels all MAS frameworks to date have fallen short.

They have not fallen short because the idea hasn't occurred to others (it certainly has), but because the ability to ensure an entire MAS is [cohesive](https://en.wikipedia.org/wiki/Cohesion_(computer_science)) is about as non-trivial a problem as exists.

Some problems require deep expertise to solve. Others require broad expertise. And some require both broad and deep expertise. It is the author's opinion that ensuring schema cohesion in a MAS is an interdisciplinary problem that cuts across the lowest levels of a system (the code actually running the MAS), to the data of the system (the database housing the cohesive data), and up to the highest level, where servers are communicating with clients on behalf of that system.

Therefore, in one sense, the answer is easy: generate an entire system from a single schema input. It is, however, easier said than done. Put in MAS terms, this is implicitly stating the need to generate entire distributed belief states for MAS dynamically, and via a single input — and to do so by ensuring that everyone along the way shares a cohesive schema.

Only with this ability to generate a distributed belief state will a MAS ensure that agents can autonomously agree upon a schema with one another, share a distributed (and historical) belief state, and also add to that belief state in real time.

## Consequences & Tradeoffs
Achieving this in Ubiquia requires the implementation of several novel techniques.

- **First**: The ability to generate a strongly-typed, JSON-validating RESTful server from a single JSON Schema input that is automatically deployed in a Kubernetes environment. The deployed server should support both in-memory SQL databases (e.g., H2) and distributed NewSQL databases (e.g., YugabyteDB), as well as Minio for object storage — all pending configuration.

- **Second**: This belief state must support the ability to RESTfully persist fully relational client payloads (represented as Agent Communication Language models defined in the JSON Schema) into the distributed database ***a posteriori*** to Ubiquia's installation.

- **Third**: This belief state must support the ability for clients to RESTfully query relational data from the database ***a posteriori*** to installation, with support for pagination, filtering, and polymorphism.

***Each of these individually, to the best of the author's knowledge, is novel — let alone their aggregation.*** It goes without saying, then, that the consequences boil down first and foremost to this: it hasn’t been done because it is an exceedingly difficult problem. If generating dynamic belief states is indeed an exceedingly difficult problem, then solving it becomes the linchpin of Ubiquia’s ultimate success — or failure.

## Contributors
- **Jeremy Case**: jeremycase@odysseyconsult.com
