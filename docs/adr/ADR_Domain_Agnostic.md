# Architecture Decision Record: Domain Agnostic Ubiquia

## Decision
Ubiquia should be "domain agnostic" in that it can realize domain-specific software solutions dynamically. To best achieve this, Ubiquia will be a microservice-based architecture where most components are inherently domain agnostic while a few specific microservices are responsible for realizing "business logic" specific to a domain. Moreover, Ubiquia will ensure cohesion within a Ubiquia domain via myriad code generation components, including both a "data contract" mechanism called the ***Agent Communication Language*** (ACL) that can generate a database interface called a "Belief State." 

Most components in a Ubiquia instance, then, will either be generated ***a posteriori*** to Ubiquia implementing a ACL. The remaining core components will exist--heterogenously--within all Ubiquia agents, regardless of any specific domain, and will be concerned with the domain agnostic functionality of Ubiquia (e.g., how to distribute in-transit compute capabilities.)

## Status 

### [1.0.0] - 2025-07-01
- Accepted.

## Summary 

### Pros
- Key enabler for globally autonomous, multi-agent system that can adapt in real-time

### Cons
- Forces core code to always be "domain agnostic"; this makes this code magnitudes more difficult to implement
- New paradigm; difficult for devs to wrap their heads around

### Alternatives
- Just don't do domain agnostic.

## Context
In order for Ubiquia to be "domain agnostic", it must be able to realize domain solutions dynamically while still maintaining its heterogenous, autonomous, distributed, multi-agent nature. To achieve domain-agnosticity, Ubiquia will utilize three key technologies: ***The Flow Service***, a ***Belief State Generator*** service, and ***Agent Communication Languages***.

The Flow Service is itself a software tool not unlike [Apache Camel](https://camel.apache.org/), [Metaflow](https://metaflow.org/), or [Airflow](https://airflow.apache.org/) in that it allows for integration of disparate software components and systems. Where it differs, however, is in two key features: that it is explicitly built off of Kubernetes and that it allows users to define static "Direct Acyclic Graphs" (DAG's) representing data flows. Via the combination of Kubernetes and these DAG's, The Flow Service can dynamically instantiate, manage, and teardown entire data flows. This is a useful property in that Ubiquia can "offload" the responsbility for defining, instantiating, and managing "business logic" to The Flow Service. 

The Belief State Generator will accept as an input the ACL. Using this ACL, the Belief State Generator will generate a "middleware component"--a domain-specific belief state--that will serve as a strongly-typed, schema-based, distributed belief state available to all components within a Ubiquia instance. This Belief State will use the ACL to generate the database entities and all endpoints for clients to use to ingress/query/update these entities automatically, without manual intervention.

## Consequences & Tradeoffs
A domain agnostic, dynamic, autonomous software system comes with many benefits - especially with respect to military applications. A system that is domain-agnostic and dynamic can--in theory--autonomously react to stimuli in the environment. Practically-speaking, such a system represents the ability to quickly adapt to changes in the environment, such as the sorts of changes expected in a military conflict. 

Such an architectural decision incurs an obvious complexity arising from software that must be generic-enough to be generalizable but also tailorable-enough to be useful for even the most technical of domains (e.g., Space.) 

Finally, the ability to generate a belief state "server component" against a relational database ***a posteriori*** to the generation of software models is--to the best of the authors' knowledge--an unsolved problem. It is an unsolved problem for a reason (i.e., it's difficult), and thus will have to be made (i.e., solved) before Ubiquia can be fully realized as a domain-agnostic software system.

## Contributors
- **Justin Fletcher**: justinfletcher@odysseyconsult.com
- **Jeremy Case**: jeremycase@odysseyconsult.com