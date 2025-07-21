# Architecture Decision Record: Agent Communication Language

## Decision
Ubiquia will have the concept of ***Agent Communication Languages*** that will reverberate through any implementation of Ubiquia.

## Status 

### [1.0.0] - 2025-07-01
- Accepted.

## Summary 

### Pros
- Acts as a Single Source of Truth (SST) for agents, DAGs, and Belief States
- Enables autonomous agent collaboration through shared semantics
- Supports runtime evolution and dynamic agent onboarding
- Enforces strong data integrity across systems (validation + verification)
- Allows runtime flexibility and composability
- Improves observability and system auditability
- Strong Data Contracts

### Cons
- Strong Data Contracts
- Requires solving unsolved problems with novel techniques
- Potential Performance tradeoffs in generated Belief State servers (Reflection, baby!)
- Few souls on the planet will have the courage to maintain much of the code implied by an ACL-first approach 

### Alternatives
- Schema-less free-for-all
- New, custom protocol beyond JSON Schema (good luck with getting community adoption)

## Context

> "You can have any Multi Agent System you want, as long as it has a Ubiquia Agent Communication Language." - Henry Ford

With the advent of LLM's, an old topic in Multi Agent Systems (MAS) has gained new prevalance: how to ensure agents within an MAS can successfully communicate.  

As a software engineer, the author has spent too many long nights and early mornings debugging production software issues that could have been prevented with robust schemas (and--by extension--their enforcement.) The author knows too well how errors compound as they propagate over a distributed system - and these were issues caused by other humans (with all of the quality assurance implied.) Presumably, these sorts of errors will approach Lovecraftian-levels of horror unless an autonomous MAS takes a "schema-first" approach. Therefore, the author believes that ***_the_*** key enabler of any (actual) production-worthy MAS are cohesive, robust, _and enforced_ schemas. These schemas are typically known  as ***Agent Communication Languages*** (ACLs) in MAS parlance.

As MAS system designed to ensure successful integration of agents into production, Ubiquia will start with an "***ACL-first***, ***ACL-always***" approach. Provided an ACL (really, a minimal amount of Ubiquia metadata around a [JSON Schema](https://json-schema.org/)), Ubiquia can orchestrate entire workflows (represented as yaml-based Directed Acyclic Graphs), and even entire distributed Belief States. Through these belief states and DAG's, Ubiquia can ensure data integrity with JSON validation and SQL data verification.

Because Ubiquia assumes ACL's as the root of all truth, Ubiquia is free to make other downstream assumptions. Namely, that it can deploy workflows (DAGs) from these schemas, ensure data flowing over these DAGs is valid per the schema, generate entire belief states per these schemas, and do all of this at runtime ***a posteriori*** to Ubiquia's installation.

## Consequences & Tradeoffs
Some developers will hate a schema-first approach. Those developers haven't debugged production distributed systems at 2 A.M. in the morning.

Less tongue-in-cheek, constraining Ubiquia DAGs and Belief States to revolve around ACL's imposes certain restrictions on Ubiquia. For one, it is difficult to implement...and many novel techniques will be need to be implemented. For example, most devs would be seduced by the allure of NoSQL databases because generating fully SQL-compliant databases (and their corresponding servers) at runtime is--to the author's best knowledge--an unsolved problem. This is just one example of the many hurdles Ubiquia will have to overcome by taking an "***ACL-first***, ***ACL-always***" approach.

## Contributors
- **Jeremy Case**: jeremycase@odysseyconsult.com