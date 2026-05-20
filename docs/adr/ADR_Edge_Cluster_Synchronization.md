# Architecture Decision Record: Edge Agent Cluster Synchronization

## Decision
Microweight (edge) Ubiquia agents will use JGroups TCP for autonomous cluster membership and peer discovery, and application-layer HTTP synchronization to propagate entity state across each agent's independent, embedded H2 database. There is no shared database. There is no central coordinator. Agents are expected to pop in and out of the cluster without orchestration.

## Status

### [1.0.0] - 2026-05-18
- Accepted.

## Summary

### Pros
- Each agent is independently operational at all times — a partition event is a normal operating condition, not a failure
- No shared infrastructure required; works in austere, degraded, or contested network environments
- JGroups TCP peer discovery is self-healing: agents that come back online automatically rejoin and synchronize
- Application-layer synchronization is transparent to the database — no NewSQL cluster management, no distributed transaction coordination
- Deployable anywhere Docker runs — no Kubernetes, no cloud, no prerequisite infrastructure

### Cons
- Consistency is eventual, not strong — agents may briefly operate on divergent state following a partition
- No built-in conflict resolution; last-write-wins semantics apply when synchronized entities are independently mutated on both sides of a partition
- Synchronization gaps are possible if an agent leaves the cluster before its unsynchronized records are pushed

### Alternatives
- **YugabyteDB xCluster**: bidirectional database replication between Kubernetes-resident agents; rejected because it requires stable Kubernetes infrastructure and assumes long-lived, addressable nodes — both of which are poor assumptions for edge deployments
- **Gossip protocol (e.g., Hazelcast)**: adds a shared distributed data grid; rejected because it still requires agents to agree on a shared memory space, which breaks down when agents are offline
- **Centralized message broker (e.g., Kafka)**: durable, ordered event log for replication; rejected because it introduces a single point of failure that contradicts Ubiquia's survivability mandate

## Context

### Context: The Edge Problem

Ubiquia is designed to operate in a military conflict, which means it must operate _at the edge_ — on hardware that is physically forward-deployed, potentially behind contested or degraded networks, and almost certainly not connected to a managed Kubernetes cluster. These "microweight" agents run wherever Docker runs: laptops, tactical servers, embedded compute nodes, vehicles. They are not orchestrated by a cloud provider. They are not guaranteed stable IP addresses. They are not guaranteed to be online.

This is a fundamentally different deployment environment from a Kubernetes-resident agent, and it demands a fundamentally different clustering strategy. Any approach that requires a cloud control plane, a managed network, or a persistent shared resource fails immediately under these constraints.

### Context: The Autonomy Problem

Ubiquia agents are expected to come and go. This is not an edge case — it is the operational baseline. An agent might be a laptop that a soldier carries onto a helicopter. It might be a compute node on a vehicle that drives out of radio range. It might be software running on a drone that is recovered, refueled, and redeployed hours later. In each case, the agent will disconnect from its cluster and later reconnect, possibly in a different network topology and possibly with a different set of peers.

A distributed database technology like YugabyteDB is designed with the opposite assumption: that nodes are long-lived, stable members of a cluster, and that losing a node is an exceptional event requiring operator intervention. Designing Ubiquia around such a technology would mean that every agent departure and arrival is a database administration event. At the edge, in a conflict, this is operationally untenable.

The autonomy problem demands that cluster membership changes are handled _automatically_ and _without operator intervention_. When an agent joins, it should synchronize. When it leaves, the remaining agents should continue operating. When it rejoins, it should pick up where it left off.

### Context: Why YugabyteDB is Moot at the Edge

YugabyteDB xCluster replication — the mechanism originally considered for cross-agent state synchronization — is designed for geographically distributed Kubernetes clusters with stable networking between them. It is a powerful tool for exactly that use case. However, it makes no provision for the following realities of edge deployments:

- Agents may be **offline for hours or days** before reconnecting
- Agents may join a **completely different cluster topology** than the one they left
- Agents may operate on **entirely separate networks** with no route between them
- Agents run on **hardware that cannot host a YugabyteDB node** (constrained embedded compute)
- There is **no Kubernetes** to manage pod scheduling, service discovery, or persistent volumes

In short: YugabyteDB is a shark technology for the cloud. At the edge, the cloud doesn't exist.

### Context: JGroups as the Discovery Layer

[JGroups](http://www.jgroups.org/) is a toolkit for reliable multicast and group membership — a proven, lightweight library used in production by Infinispan, WildFly, and others for decades. It provides exactly what microweight agents need: automatic peer discovery, membership change notifications, and failure detection, without any external coordinator.

For microweight agents, Ubiquia uses JGroups TCP with `TCPPING` — a static seed-host list that agents use to bootstrap initial peer discovery. Once a channel is formed, JGroups manages membership autonomously: when a new agent appears, the view updates; when one disappears, the view updates again. The application receives these view-change events and reacts accordingly (see `MicroweightNetworkManager`).

A deliberate design detail: agents that find themselves alone in the cluster (a single-member view) will periodically disconnect and reconnect their JGroups channel. This prevents a permanent "split" where two agents are each running solo and have missed one another's bootstrap window. Jitter is applied to reconnect attempts so that agents starting simultaneously stagger their retries and find one another reliably.

### Context: Application-Layer Synchronization as the Replication Layer

Because each agent maintains its own embedded H2 database, there is no shared persistence layer to act as the source of truth across agents. Replication is instead performed at the application layer by `ClusterSynchronizationService`, which runs on a fixed schedule and pushes entity state to all known peers via HTTP.

Each synchronizable entity type has a corresponding `AbstractSynchronizationService` implementation that:
1. Queries the local database for records that have not yet been pushed to peers
2. Maps those records to DTOs
3. POSTs each DTO to each peer's REST endpoint
4. Records a `SyncEntity` to mark the record as synchronized — but only if _all_ peers acknowledged the push

This design means that synchronization is idempotent and conservative: if a peer is unreachable, the record remains in the "needs sync" queue and will be retried on the next cycle. No data is marked synchronized until it has been successfully delivered.

### Context: Network Consolidation

A consequence of agents operating with independent databases is that each agent maintains its own notion of "which network do I belong to?" — represented by a `NetworkEntity`. When multiple agents connect via JGroups, their individual networks must be reconciled into a single canonical network so that the agents can share a peer list and synchronize entity state coherently.

`MicroweightNetworkManager` performs this consolidation. When a multi-member cluster view is observed, it elects the oldest `NetworkEntity` (by creation time) as canonical and migrates all known agents to it, deleting orphaned networks. When an agent finds itself alone, it ensures it has its own solo network. This is a simple, deterministic policy that avoids split-brain ambiguity without requiring a consensus protocol.

## Consequences & Tradeoffs

The primary consequence of this design is that Ubiquia's edge agents are **eventually consistent**, not strongly consistent. During a partition — which, again, is an expected operating condition — agents may act on divergent state. When the partition heals, the synchronization cycle will propagate the delta. In practice, because Ubiquia's entities (domain ontologies, graphs, agent registrations) are largely additive rather than mutually exclusive, divergence is manageable. The greater operational risk is a synchronization gap — a record created just before an agent departs that never reaches its peers — and this is accepted as the cost of an infrastructure-free deployment model.

The upside is significant: microweight agents require no external infrastructure beyond Docker. They carry their entire state with them. They can operate completely offline, reconnect to a cluster days later, and synchronize automatically. In a contested environment where infrastructure is the first casualty, this self-sufficiency is not a "nice to have" — it is the entire point.

## Contributors
- **Jeremy Case**: jeremycase@odysseyconsult.com
