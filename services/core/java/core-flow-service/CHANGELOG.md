# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.30.0] - 2026-05-18
### Added
- `IntraKubernetesReplicaClusterService`: JGroups KUBE_PING channel providing leader election among K8s pod replicas; only the elected leader runs scheduled work (sync, egress relay update, heartbeat)
- `IntraKubernetesHeartbeatService`: periodic `/actuator/health` probing of remote agents; marks agents `reachable=false` after a configurable failure threshold and lifts tombstones on recovery; conditional on `ubiquia.kubernetes.enabled=true`
- `IntraKubernetesSynchronizationService`: queries the local database for agents in the same network with a non-null `baseUrl` and `reachable=true` to build the list of K8s peer URLs for the sync cycle
- `jgroups-kube.xml`: JGroups protocol stack config using `KUBE_PING` for automatic pod discovery within the cluster namespace
- `jgroups-kubernetes:2.0.1.Final` runtime dependency

### Changed
- `ClusterSynchronizationService`: injected `Optional<IntraKubernetesReplicaClusterService>`; `synchronize()` and `tryBuildEgressRelays()` now skip if not leader; `resolvePeerUrls()` now combines microweight JGroups URLs with `IntraKubernetesSynchronizationService` DB-sourced URLs; `KubernetesSynchronizationService` field renamed to `intraKubernetesSynchronizationService`
- Package reorganization — `cluster.synchronization` subpackages introduced:
  - `entity`: `AbstractSynchronizationService`, `AgentSynchronizationService`, `DomainOntologySynchronizationService`
  - `microweight`: `MicroweightNetworkManager`, `MicroweightClusterService`, `MicroweightSynchronizationService`
  - `kubernetes`: `IntraKubernetesReplicaClusterService`, `IntraKubernetesHeartbeatService`, `IntraKubernetesSynchronizationService`

### Removed
- `jdbc-yugabytedb` runtime dependency
- `KubernetesReplicaClusterService`, `KubernetesHeartbeatService`, `KubernetesSynchronizationService` from `cluster` package (replaced by `IntraKubernetes*` equivalents in `cluster.synchronization.kubernetes`)

## [0.29.0] - 2026-05-15
### Fixed
- `QueueNodeTest`: updated hardcoded endpoint URLs from `graph/{graph}/node/{node}/queue/{peek,pop}` to `ubiquia/core-flow-service/{graph}/node/{node}/queue/{peek,pop}` to match the new path registered by `NodeEndpointRecordBuilder`

## [0.28.2] - 2026-05-13
### Fixed
- `FlowEgressRelayTest`: call `relay.teardown()` in `@BeforeEach` to cancel the background scheduler before each test, eliminating the intermittent `ObjectOptimisticLockingFailureException` caused by the 500 ms poll task racing with explicit `tryPollAndForward()` calls to delete the same `FlowMessageEntity`

## [0.28.1] - 2026-05-08
### Changed
- `GraphRegistrar.tryAdaptComponentsToNodes()`: added reverse-direction pass over `graph.getNodes()` that links a node to its component via `node.getComponent()` when `component.getNode()` is null; fixes cardinality-based node skipping for agents that receive the graph via sync rather than registering it from YAML
- All registrars (`GraphRegistrar`, `NodeRegistrar`, `ComponentRegistrar`, `DomainOntologyRegistrar`, `DomainDataContractRegistrar`): incoming DTO `id` is now preserved on newly-created entities so synced records share the same UUID as the originating agent
- `FlowClusterService`: `bind_addr` is now configurable via `ubiquia.cluster.bind-addr` (default `GLOBAL`); value is propagated to JGroups via the `jgroups.bind_addr` system property
- `jgroups-tcp.xml`: `TCP` element now reads `bind_addr` from `${jgroups.bind_addr:GLOBAL}`
- `FlowMessageController`, `FlowMessageRegistrar`, `NodeManager`, `FlowEgressRelay`: key diagnostic log statements promoted from DEBUG to INFO

## [0.28.0] - 2026-05-05
### Added
- `FlowEgressRelay`: prototype-scoped component that polls for `FlowMessageEntity` records targeting nodes not locally deployed and forwards them to configured peer agents via `POST /flow-message/receive`; one instance is created per peer agent by `UbiquiaSynchronizationService`
- `FlowEgressFactory`: factory that instantiates `FlowEgressRelay` prototype beans
- `FlowMessageController`: REST controller exposing `POST /ubiquia/core/flow-service/flow-message/receive`; accepts a `FlowMessage` DTO and delegates to `FlowMessageRegistrar`
- `FlowMessageRegistrar`: creates the `FlowEntity` / `FlowEventEntity` / `FlowMessageEntity` chain when a flow message is received
- `NetworkManagementService`: manages `NetworkEntity` membership for agents (supports simulation partition scenarios)
- `NetworkRepository`: JPA repository for `NetworkEntity`
- `AbstractSynchronizationService<E, D>`: generic base class for typed entity synchronization services
- `DomainOntologySynchronizationService`: concrete sync implementation that propagates `DomainOntologyEntity` records to peer agents
- `UbiquiaSynchronizationService`: scheduled orchestrator (conditional on `ubiquia.cluster.flow-service.sync.enabled=true`) that drives all registered `AbstractSynchronizationService` implementations and initialises `FlowEgressRelay` instances; replaces `ModelSynchronizationService`
- `NodeManager.getLocalNodeIds()`: returns `Set<String>` of node IDs currently active in the local node map
- `FlowMessageRepository.findAllByTargetNodeIdNotIn(Collection<String>, Pageable)`: paged query for orphaned flow messages

### Removed
- `ModelSynchronizationService`: superseded by `AbstractSynchronizationService` / `DomainOntologySynchronizationService` / `UbiquiaSynchronizationService`

### Fixed
- `FlowEgressRelay.forwardOrphanedMessages()`: guards against Hibernate 6's empty-collection `NOT IN` producing `NOT (1=1)` by falling back to `findAll(pageable)` when the local node set is empty
- `FlowMessageRegistrarTest`: added required `inputSubSchemas` and `outputSubSchema` to the PUSH-type test node, resolving `IllegalArgumentException` from node validation

## [0.26.0] - 2026-04-24
### Fixed
- `GraphRepository`: duplicate-deployment query now includes `graphName` as a discriminator, preventing false conflicts when multiple graphs from the same domain ontology and version are deployed to the same agent
- `GraphController.tryDeployGraph()`: passes `graphName` to the repository check; logs an error when a duplicate is detected and info when deployment succeeds

## [0.25.0] - 2026-04-10
### Fixed
- `ModelSynchronizationService.trySync()`: replaced `atLeastOnePeerSucceeded` with `allPeersSucceeded`; a `SyncEntity` is now only persisted when every configured peer has accepted the sync payload, preventing peers that are temporarily unreachable at startup from being permanently skipped on subsequent sync cycles

## [0.23.0] 2026-04-08
### Added
- `DomainOntologyRepository.findByName(String name)`: look up a domain ontology entity by name
- `FlowClusterService.rejoinIfSolo()`: scheduled probe that disconnects and reconnects when this node is the sole cluster member, using random jitter in `[0, rejoin-delay-ms)` to stagger simultaneous reconnect attempts; configurable via `ubiquia.cluster.rejoin-delay-milliseconds` (default 5000)

### Changed
- `ModelSynchronizationService` now syncs only `DomainOntologyEntity` records to peers; registering a domain ontology cascades creation of all child entities, so per-type sync of data contracts, graphs, nodes, and components has been removed

## [0.22.0] 2026-04-01
### Added
- `SimulationController`: REST endpoint `POST /ubiquia/core/flow-service/simulation/clock/set` to update the service clock via `ClockService`; conditional on `ubiquia.mode != PROD`
- `ubiquia.mode` property in `application.yaml` (default `PROD`), propagated from Helm configmap

## [0.21.0] 2026-03-30
### Added
- JGroups TCP cluster channel management (`FlowClusterService`) for peer-to-peer communication between flow-service instances
- Scheduled model synchronization service (`ModelSynchronizationService`) that propagates stale entities to cluster peers via HTTP
- JGroups TCP configuration file (`jgroups-tcp.xml`) with TCPPING for static host discovery
- `SyncRepository` and `ObjectMetadataRepository`
- Cluster sync toggle (`ubiquia.cluster.flow-service.sync.enabled`) with configurable sync frequency

## [0.19.0] 2026-03-23
### Added
- Docker Compose configuration for bare-metal single-service deployment
### Changed
- Bootstrap conditional properties split: `bootstrap.enabled` replaced by `bootstrap.belief-state.enabled` and `bootstrap.domain-ontology.enabled` for independent control of each bootstrapper
- Config key renamed from `flowService` to `flow-service` for consistency with Spring Boot kebab-case convention
- Fixed `application.yaml` structure: `server.port` was incorrectly nested under `spring`, now at root level
- Default log levels reduced from DEBUG to INFO for `org.ubiquia` and `org.springdoc`

## [0.18.0] 2026-03-18
### Changed
- Micrometer version now managed by Spring Boot BOM

## [0.16.1] 2026-03-03
### Fixed
- Null pointer exception. Clearly Java's fault, and not the author.

## [0.16.0] 2026-03-02
### Added
- New Update Entity class to track multi-agent updates

## [0.10.0] 2025-10-06
### Changed
- BatchId is now FlowId

## [0.9.0] 2025-10-03
### Added
- Cardinality for DAG deployments

## [0.8.10] 2025-10-01
### Fixed
- Endpoint of the ACL controller

## [0.8.7] 2025-09-05
### Fixed
- Small logging issues

## [0.8.0] 2025-09-03
### Changed
- Adapters no longer require components at all and associated code changes

## [0.7.0] 2025-08-05
### Added
- Retries into inter-service boostrapping

## [0.1.0] 2025-05-06
### Added
- Initial commit
