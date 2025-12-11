# Architecture Decision Record: Flow Service

## Decision
Ubiquia will leverage the Flow Service core service to managed client-defined data flows. The Flow Service will expose API's to allow the dynamic manipulation of these data flows (AKA, in-transit compute), and other Ubiquia services will actuate these API's.

## Status 

### [1.0.0] - 2025-07-01
- Accepted.

## Summary 

### Pros
- Application logic (and development) is decoupled from Ubiquia
- Ability to maintain "domain agnostic" Ubiquia where domain implementations are handled by Flow Service
- Location-aware compute allows for computation to occur where it's most optimal
- Dynamic runtime capability to instantiate computational workflows

### Cons
- Complexity and runtime-deployment of both application code and Kubernetes resources

### Alternatives
- Manual deployment of application logic alongside Ubiquia installation; presumably as Helm charts

## Context
Ubiquia is to be a distributed software system comprised of agents. Collectively, the agents can coalesce into networks that will be more than the sum of their parts. Specifically, this means that agents will be able to coordinate and "specialize" on specific tasks. For example, if one agent in a network is adjacent to a sensor that produces large binary data--and that binary data is unnecessary after being processed--it makes sense for that agent to process the binary data to avoid the latency costs incurred from sending that data across the network.

Not only is Ubiquia to be a multi-agent, distributed software system, it is one designed to be domain-agnostic. It is designed to be able to realize business logic "at runtime", and to do in a way that is autonomous. Moreover, via this autonomy, Ubiquia is designed such that it can "learn" and become more efficient at the realization of this business logic over time. In other words, Ubiquia will leverage fitness functions to optimize _how_ it realizes business logic at runtime.

This is all sounds like magic. Black magic, even...except that anything that is in theory possible is by extension possible with software. The only physical laws software is beholden to are those that define how electrons fly across transistors and wires. The question is not "can this be done", but "how." If Ubiquia is to be a system that can realize "runtime" business logic--and grow more efficient at it over time--then it needs a mechanism to orchestrate and manage runtime business logic.

Ubiquia agents can implement ***Agent Communication Languages*** (***ACL's***); large swaths of a Ubiquia agent will be generated only after doing so (i.e., ***a posteriori***). The Flow Service the service that handles this. Once an ACL has been registered with the Flow Service, clients (developers, other software systems, or even LLM's) can use reference it to define data flows via yaml files. Once codified in this manner, The Flow Service can then manage these data flows at runtime by interfacing with the Kubernetes API server. In effect, Flow Service doubles both as a mechanism for clients to codify data flows and also as a [Kubernetes Operator](https://kubernetes.io/docs/concepts/extend-kubernetes/operator/).  

The Flow Service will allow clients to configure user-defined logic in data flows. In Flow Service parlance, developers can deploy ***Components*** within ***Directed Acyclic Graphs*** (DAG's), and then apply ***Adapters*** to the components so that data flows across the graphs in a directed manner. Conceptually, components are user-defined, containerized logic that will accept some model--defined in an Agent Communication Language--and "transform" it, thus outputting some other model (also defined in the ACL.) 

The Flow Service can--at runtime--deploy these DAG's by interfacing with the Kubernetes API server. Because Flow Service can leverage a distributed database cluster--itself spread across an arbitrary number of Ubiquia agents--The Flow Service need not run all of the components or nodes of a graph in a single Ubiquia instance. Put differently, the Flow Service can instantiate specific components of a DAG within the Ubiquia agents best-suited to run those components. 

Importantly, The Flow Service can instantiate and/or teardown DAG's at runtime. Conceivably, a network of Ubiquia agents might be tearing down/spinning up Flow Service DAG's continuously in response to stimuli in the computing environment.

## Consequences & Tradeoffs
The Flow Service will use the distributed database almost as if were a message broker. It will treat messages flowing over a graph as a message queue, replete with transactions (again, possible because the database is both distributed and transactional.) This is slower than a point-to-point pattern as well as a message broker pattern. But this approach is only slower than alternatives "at surface level"; this approach allows for Flow Service to define "back pressure" so that Ubiquia's executive service can delegate to Kubernetes to "horizontally autoscale" to relieve this back pressure. In other words, this approach serves as the foundation for Ubiquia's autonomous, dynamic, autoscaling. 

The Flow Service offers the knobs through which a service--like Ubiquia's executive service--can be used to handle runtime optimization. But it doesn't have any opinions as to how these optimizations should be made. An entirely-different component of Ubiquia will need to actually actuate Flow Service towards optimizating a network of Ubiquia agents. Similarly, The Flow Service does not define the fitness functions that will be used to determine whether one optimization approach is superior to another; that, too, will be left to other components.

## Contributors
- **Jeremy Case**: jeremycase@odysseyconsult.com