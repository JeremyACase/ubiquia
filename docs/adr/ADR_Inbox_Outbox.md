# Architecture Decision Record: Inbox/Outbox Pattern

## Decision
Ubiquia will use an inbox/outbox microservice pattern for its Flow Service.

## Status 

### [1.0.0] - 2025-07-01
- Accepted.

## Summary 

### Pros
- Discrete transactions
- Allows for communication across Kubernetes clusters (provided a distributed database)

### Cons
- "Latency" introduced by the requirement on discrete transactions
- Requirement on a database; a distributed one at that (at least in prod)

### Alternatives
- A broker distributed across multiple Kubernetes clusters 

## Context

> "You get a box! And you get a box! And you all get boxes!" - Oprah Winfrey

### Context: Message Brokers and Kubernetes Clusters

Like the (in)famous [Gang of Four Design Patterns](https://en.wikipedia.org/wiki/Design_Patterns) that are primarily-concerned with code-level patterns, there are [Microservice Patterns](https://microservices.io/patterns/) that are concerned with systems-level patterns. One such pattern is the Inbox/Outbox pattern, which will be used in Ubiquia.

Ubiquia is designed to be a [multi-agent system](https://en.wikipedia.org/wiki/Multi-agent_system). To achieve this design constraint, Ubiquia assumes a 1:1 relationship between a Kubernetes cluster and a Ubiquia agent. At a high-level, every agent becomes an island connected to other islands via a "logical" distributed database cluster. Because agents are islands "connected" by a distributed database cluster, Ubiquia will leverage the inbox/outbox pattern to do discretized, atomic communications.

Within a Kubernetes cluster, it is a very-common design to use a [message broker](https://en.wikipedia.org/wiki/Message_broker). Brokers in this manner are "intra-cluster" in that within a Kubernetes cluster, a broker can publish messages to any subscribed microservices. What they cannot do--at least not easily--is to publish these messages to any subscribed services in another Kubernetes cluster. _This is especially true in the heterogenous world of DoD software (with all of the Cyber regulations implied.)_

### Context 

The Flow Service is designed to work across arbitrary-scales; it does this by assuming that the database is distributed. Adapters will effectively communicate with one another over this distributed database. 

Because Flow Service nodes leverage the transactional-nature of a distributed SQL database, they can send messages in a discretized, transactional manner, agnostic of where the nodes reside (i.e., in the same Kubernetes cluster, or in another Kubernetes cluster.) 


## Consequences & Tradeoffs

Inbox/outbox patterns require some mechanism to retrieve messages. The simplest case is for services to "listen" to incoming messages--i.e., to retrieve targeted messages--over the database is to simply poll them. Polling is slower than alternatives, of course, but it is effective and doesn't require much code.

An alternative is to create a "push" mechanism over the inbox/outbox. This can be realized via a [changefeed-feature](https://www.cockroachlabs.com/docs/stable/changefeed-examples), or maybe a [database trigger](https://en.wikipedia.org/wiki/Database_trigger). In either case, both approaches will take significant developer time. 


## Contributors
- **Jeremy Case**: jeremycase@odysseyconsult.com