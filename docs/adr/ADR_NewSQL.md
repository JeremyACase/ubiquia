# Architecture Decision Record: NewSQL

## Decision
Ubiquia will allow for a NewSQL distributed database as the persistence layer to any given Ubiquia Agent's "Belief State" as well as for Ubiquia itself to managed data flows. When configured this way, this persistence layer will itself be globally-distributed cluster where nodes within the cluster reside within Ubiquia agents.

## Status 

### [1.0.0] - 2025-07-01
- Accepted.

## Summary 

### Pros
- Eventually-consistent
- Distributed across Kubernetes clusters; key enabler of inbox/outbox pattern
- SQL compliant (piggy-backing on decades of well-understood industry experience)
- Survivability; can survive loss of parts of the system
- Cloud-native; supports configuration via Helm

### Cons
- Slower than a monolithic database
- Complexity inherent due to distribution of data

### Alternatives
- NoSQL distributed databases
- Distributing "classical" SQL databases 

## Context
Ubiquia is a software system designed to survive a military conflict. Like all software systems, Ubiquia needs data to be useful, and like Ubiquia itself, that data needs to be able to survive a military conflict. Ubiquia is itself a multi-agent, heterogenous, autonomous software system. While every Ubiquia agent needs to "be its own universe", the database that those agents use need not be. Whatever database Ubiquia uses, it needs to be able to solve a lot of tricky, distributed database problems such as leadership election, consistency, ACID-ity, etc...to say nothing of the ability to survive a military conflict.

Moreover, this "primary database" should use strict schemas as the use of those schemas leads to the ability to upgrade/evolve data over time and ensure a good quality of said data (the sort of quality a military operator would expect from an operational weapon system.) This sort of schema requirement heavily implies an SQL database over other competing technologies (such as NoSQL.) 

Minus the possibility of kinetic strikes, database resiliency and consistency at global scale are the same problems faced by many of the world's largest tech companies. These companies have thought long and hard about these problems and have developed resilient, distributed, scalable, eventually-consistent SQL database technologies to solve them. Rather than roll our own database technology, Ubiquia will leverage one such "NewSQL" technology. Ubiquia was originally designed to use CockroachDB, but CockroachDB has since changed its licensing. Accordingly, it will instead use YugabyteDB.

Every Ubiquia agent will have a belief state microservice, but these belief states may themselves be pointed to a NewSQL "database cluster" that lives "outside" of the Ubiquia Agent (even if some of the cluster's nodes live within the agent.) 

## Consequences & Tradeoffs
Being a newer technology than its more-classical analogs (like MySQL or Postgres), there is a steep learning curve implied with a NewSQL database (i.e., YugabyteDb.) This is especially true due to it being fundamentally a distributed database, something that is not built into the more-classical SQL databases like MySQL and Postgres (but can be "bolted on" - albeit in a clunky, inelegant way.)

In addition to the difficulty curve implied by such a technology, the usage of YugabyteDB within an autonomous Ubiquia leverages a requirement that Ubiquia agents must be able to manage YugabyteDB: namely the creation of YugabyteDB nodes and the act of joining of them into singular clusters. The real challenge of this approach is not in the joining of YugabyteDB nodes into a cluster, however. The real challenge lies in the requirement of Ubiquia to be resilient in a conflict - or the expectation that Ubiquia agents sharing a database cluster might be "severed." Put differently, Ubiquia is designed such that a network of agents should be able to survive "partition events" where the database cluster is bifurcated into smaller networks.

To be resilient in a conflict while maintaining a consistent "Belief State", Ubiquia agents must be able to manage database clusters should they get "severed" from all other agents (up-to-and-including instantiating entirely-new clusters.) The even-more difficult implication of this is that disconnected agents coming into contact with one another need to be able to agree how to consolidate two disparate database clusters together (i.e., reconcile them), and to do so autonomously.


## Contributors
- **Jeremy Case**: jeremycase@odysseyconsult.com
- **Justin Fletcher**: justinfletcher@odysseyconsult.com