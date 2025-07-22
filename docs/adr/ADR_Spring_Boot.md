# Architecture Decision Record: Use of Spring Boot for Core Ubiquia Services

## Decision
Ubiquia will use Spring Boot (Java) for its core microservices (while reserving the use of other languages where appropriate - like Python for utility services, or TypeScript for user interfaces.)

## Status 

### [1.0.0] - 2025-07-01
- Accepted.

## Summary 

### Pros
- Robust, mature, modern (see: supported) framework
- JVM 
- Peerless enterprise-grade database ecosystem (Hibernate, Hikari, JPA, etc.)
- Best-in-industry infrastructure integration (opinionated--autowired--plugins for Kubernetes, Grafana, Prometheus, Istio, databases, brokers, etc.) 
- Future-proofed (Netflix, Amazon, most FinTech, and many other companies run their businesses primarily on Spring Boot)

### Cons
- Not widely adopted in the MAS or AI community as Python frameworks
- Perceived as "Heavyweight" 
- Learning Curve 

### Alternatives
- Python & Django
- Python, Flask, SQLAlchemy, 
- Python & FastAPI
- Quarkus
- Node.JS
- Micronaut
- Go (and associated frameworks)

## Context

> ***Users don't care about software, they care about data.*** - Ubiquia Author

It is a highly-idiosyncratic choice to build a Multi Agent System (MAS) in Spring Boot Java rather than Python and a Python framework. However, most analogous frameworks ([Airflow](https://airflow.apache.org/), [LangGraph](https://github.com/langchain-ai/langgraph), [Agents SDK](https://github.com/openai/openai-agents-python), etc.) are concerned with a subset of the problems Ubiquia is, and they are primarily concerned with the developer experience as it relates to application programming. Put differently, these frameworks seek to lower the barrier of entry for devs to easily orchestrate agents and/or workflows into ***applications***. 

Ubiquia takes a slightly different approach: Ubiquia is concerned primarily with lowering the barrier of integrating agent-based workflows quickly and autonomously into ***production-worthy systems***. Therefore, Ubiquia takes the idiosyncratic approach to use Spring Boot to write a Multi Agent System (MAS) framework.

Fundamentally, this is the underlying assumption of Ubiquia: ***An autonomous system of agents will collapse under scale unless schemas are treated with reverence...from the lowest-level, to the highest level***. Ubiquia assumes data is sacred: it must be high-quality, accurate, and [normalized](https://en.wikipedia.org/wiki/Database_normalization) if the data is to work in production. Therefore, not only will the agents communicate using schemas, but the underlying languages will ensure ***guard rails*** in the form of a strongly-typed language (in this case, Spring Boot Java.) While quality of data is a prerequisite of any production software system, it is especially important for one that deploys autonomous agents and integrates LLM-based workflows.

Ubiquia is many things. But it is also a runtime substrate for intelligent agents; it importantly has no opinions on how or what the ***components*** within its ***Directed Acyclic Graphs*** (DAGs) are. They can be Python black boxes, LLM's, or even entire software systems. All Ubiquia cares about is that these ***components*** have an input and an output that maps to a predefined ***Agent Communication Language***. Put bluntly: Ubiquia doesn't prevent devs from using Python...or any other language should they want to use it.

The author would go so far as to argue that if Ubiquia has a competitive edge over any analogous framework (and the author is unaware of any such framework that simultaneously addresses all the problems Ubiquia does), it's this: ***Ubiquia's choice of a boring, stodgy, enterprise framework is perhaps its greatest strength***. 

## Consequences & Tradeoffs
As with all things in software engineering, assuming Spring Boot as the core framework is a tradeoff. Ubiquia is optimizing for successful integration into production environments, but at the cost of the ability for many AI & ML devs to understand the codebase. Spring Boot and Java is typically the realm of back-end software engineers, site reliability engineers (SREs), and infrastructure engineers. Compared to the freedom enjoyed by Python devs, Spring Boot Java is likely to seem onerous, verbose, and clunky. ***It is all of those things for good reason***. For this reason, one assumed consequence of Spring Boot is thus: ***AI & ML engineers are unlikely to be able to maintain--let alone add features to--the core parts of Ubiquia***.

This isn't by design per se, but it is thematic with Ubiquia: Ubiquia seeks to make the "difficult infrastructure stuff" transparent to AI & ML engineers. Ubiquia's approach is thus: once a user "composes" a DAG, it should "just work" in Ubiquia. Therefore, Ubiquia assumes users composing DAGs should not have to worry about Java, or Spring Boot...let alone Kubernetes, inbox/outbox queues, YugabyteDB, Helm, database tables, or anything else. Ubiquia seeks to allow AI & ML engineers to easily integrate their agent-based workflows into Ubiquia as DAGs without ever having to worry about Ubiquia internals.  


## Contributors
- **Jeremy Case**: jeremycase@odysseyconsult.com