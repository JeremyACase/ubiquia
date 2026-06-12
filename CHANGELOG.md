# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.37.0] - 2026-06-12
### Changed
- `core-belief-state-generator-service`: introduced `SchemaTransformationPipeline` (Chain of Responsibility) and `SchemaTransformer` interface; schema preprocessing is now a sequenced, Spring-ordered list of transformers (`EnumNormalizer`, `UbiquiaModelInjector`, `InheritancePreprocessor`) rather than inline ad-hoc mutations
- `core-belief-state-generator-service`: extracted `AbstractOpenApiGenerator` (Template Method) as a shared base for `OpenApiDtoGenerator` and `OpenApiEntityGenerator`; generator name, template path, additional properties, and global properties are now supplied via hooks, eliminating duplicated `CodegenConfigurator` setup
- `core-belief-state-generator-service`: introduced `K8sLabelBuilder` fluent builder for Kubernetes label map construction; replaces ad-hoc `Map.of()` / `HashMap` usage in `BeliefStateDeploymentBuilder` and `BeliefStateOperator`
- `core-belief-state-generator-service`: introduced `K8sResourceClient<T, L>` generic wrapper around `GenericKubernetesApi` that binds a namespace at construction time; `BeliefStateOperator` now holds typed clients instead of raw APIs, removing repeated namespace arguments at every call site
- `core-belief-state-generator-service`: `BeliefStateGenerationSupportProcessor` support-file registry converted to a package-private `SupportTemplate` enum with `resourcePath`, `destinationPath`, and `requiresReplacement` fields

### Fixed
- `InheritancePreprocessor.transform()`: was returning the original schema string instead of the modified one; now correctly returns the post-transformation result
- `InheritancePreprocessor.processContainer()`: `AbstractDomainModel` and `KeyValuePair` are now excluded from `allOf` injection; previously `AbstractDomainModel` received a self-referencing `allOf` (causing a duplicate `getModelType()` method in every generated class) and `KeyValuePair` received a circular `allOf` reference (breaking its EMBEDDABLE classification)
- `AbstractOpenApiGenerator.generate()`: added `openApiNullable=false` to prevent `JavaClientCodegen` from wrapping nullable fields in `JsonNullable<>` when `jackson-databind-nullable` is absent from the generated project's classpath
- `UbiquiaModelInjector`: removed `modelType` from the injected `AbstractDomainModel` schema; the DTO and entity Mustache templates already generate `@Override getModelType()`, so the schema property was producing a duplicate method compile error in all generated classes
- `BeliefStateGenerationCleanupProcessor`: removed `KeyValuePair.java` and `KeyValuePairEntity.java` from the blacklist; generated domain models import these types from `org.ubiquia.domain.generated` and require them on the compilation classpath; support files (`Controller`, `Repository`, `RelationshipBuilder`, `DtoMapper`) are already deleted by the generators' own `postProcessFile` logic for embeddable models

## [0.35.0] - 2026-05-27
### Added
- YugabyteDB `heavyweight` deployment tier: `prod/heavyweight.yaml`, `test/integration-test-yugabyte.yaml`; uses YugabyteDB YSQL (PostgreSQL-compatible JDBC on port 5433) instead of H2 or PostgreSQL
- YugabyteDB Helm dependency (`yugabyte 2025.2.3` from `https://charts.yugabyte.com`), conditioned on `ubiquia.agent.database.yugabyte.enabled`
- Busybox init container on core-flow-service Deployment that blocks startup until YugabyteDB tserver port 5433 is reachable; only rendered when `ubiquia.agent.database.type` is `YugabyteDB`
- `integration/yugabyte/ubiquia_test_heavyweight_yugabyte.yaml`: Helm test that verifies the core-flow-service health endpoint reports a PostgreSQL-compatible datasource (YugabyteDB YSQL) and `"status":"UP"` when deployed with the heavyweight config; gated by `testing.yugabyte.enabled`
- `integration-test-yugabyte` CI job in `devops.yml` (currently disabled via `if: false`): deploys with `integration-test-yugabyte.yaml` and runs the YugabyteDB heavyweight integration test in isolation

### Changed
- `postgresql.enabled` and `yugabytedb.enabled` moved from top-level Helm values into `ubiquia.agent.database.postgres.enabled` and `ubiquia.agent.database.yugabyte.enabled`; `Chart.yaml` dependency conditions updated accordingly
- All deployment configuration overlays (`prod/middleweight.yaml`, `prod/heavyweight.yaml`, `dev/middleweight-dev.yaml`, `test/integration-test-postgres.yaml`, `test/integration-test-yugabyte.yaml`) updated to set the enabled flag under `ubiquia.agent.database`
- YugabyteDB datasource branch added to the core-flow-service configmap: `ubiquia.agent.database.type: YugabyteDB` renders the PostgreSQL JDBC driver with `jdbc:postgresql://{{ .Release.Name }}-yb-tservers:5433/...`

## [0.34.0] - 2026-05-26
### Added
- PostgreSQL Helm dependency (bitnami/postgresql 16.4.4), conditionally enabled via `postgresql.enabled`; `ubiquia.agent.database.type` value (`H2` or `Postgres`) selects the datasource in the core-flow-service configmap at render time
- `middleweight` deployment configurations: `prod/middleweight.yaml`, `dev/middleweight-dev.yaml`, `test/integration-test-postgres.yaml`; use Postgres instead of the default in-memory H2 database
- Busybox init container on the core-flow-service Deployment that blocks startup until PostgreSQL port 5432 is reachable; only rendered when `ubiquia.agent.database.type` is `Postgres`
- `integration/postgres/ubiquia_test_middleweight_postgres.yaml`: Helm test that verifies the core-flow-service health endpoint reports a PostgreSQL datasource when deployed with the middleweight config; gated by `testing.postgres.enabled`
- `integration-test-postgres` CI job in `devops.yml`: deploys with `integration-test-postgres.yaml` and runs the PostgreSQL integration test in isolation
- `scenario-tests` CI job in `devops.yml`: runs scenario Helm tests against the base H2 deployment

### Changed
- Helm test templates reorganized into `systems/`, `integration/postgres/`, and `scenario/` subdirectories; simulation tests moved from `systems/` to `scenario/`
- CI job `helm-test` renamed to `systems-tests`; `push-images` now depends on `systems-tests`, `integration-test-postgres`, and `scenario-tests`

### Fixed
- `BeliefStateOperator.tryDeployBeliefState()`: existence check was keyed on `getName().toLowerCase() + getVersion()` (e.g. `"pets1.2.3"`) instead of the canonical Kubernetes resource name from `BeliefStateNameBuilder`; injected `BeliefStateNameBuilder` and replaced the check with `getKubernetesBeliefStateNameFrom(domainOntology)`, eliminating intermittent `AlreadyExists` 400 errors on repeated deployments

## [0.33.0] - 2026-05-25
### Added
- `SyncController`: `GET /ubiquia/core/flow-service/sync/query/params` endpoint for paginated, filterable queries over `SyncEntity` records; supports dot-notation nested field filters (e.g. `sourceAgent.id=<uuid>`)
- `ubiquia_test_simulation_agent_synch.yaml`: Helm test verifying two-agent entity synchronization — bootstraps the `pets` ontology to `synch-flow-service-a`, confirms `synch-flow-service-b` receives it via JGroups sync, then confirms `SyncEntity` records exist on agent-a filtered by `sourceAgent.id`

### Changed
- `GenericDtoMapper.setAbstractEntityFields()`: no longer hydrates the `syncs` lazy collection on egress; callers querying sync status should use `GET /sync/query/params?sourceAgent.id=<uuid>` instead

### Fixed
- `ExtraKubernetesHeartbeatServiceTest.assertCheckPeerHealth_belowFailureThreshold_doesNotTombstone`: added `@MockBean TaskScheduler taskScheduler` to prevent the background `@Scheduled` probe (which uses a 3-second connection timeout per peer) from racing with `@BeforeEach` field resets and accumulating extra failure counts before the test's own `checkPeerHealth()` call
- `SyncMappingTest.assertSyncsAreMapped_isValid`: updated assertions to match the new mapper contract — verifies `dto.getSyncs()` is null (no lazy hydration) and asserts the `SyncEntity` record was persisted correctly via `SyncRepository`

## [0.32.0] - 2026-05-22
### Added
- `FlowEventSynchronizationService`, `FlowMessageSynchronizationService`, `NetworkSynchronizationService`, `ObjectMetadataSynchronizationService`: typed entity sync services extending `AbstractSynchronizationService` for cluster-wide propagation of `FlowEvent`, `FlowMessage`, `Network`, and `ObjectMetadata` records
- `FlowEventRegistrar`: idempotent registration of `FlowEvent` DTOs received from peer agents; resolves parent `Flow` and `Node` references and preserves the source UUID
- `NetworkRegistrar`: idempotent registration of `Network` DTOs received from peer agents
- `ObjectMetadataRegistrar`: idempotent registration of `ObjectMetadata` DTOs received from peer agents; resolves the parent `Agent` reference
- `FlowEventController.register()`: `POST /ubiquia/core/flow-service/flow-event/register/post` sync endpoint delegating to `FlowEventRegistrar`
- `NetworkController`: new `GenericUbiquiaDaoController` for `NetworkEntity` with a `POST /ubiquia/core/flow-service/network/register/post` sync endpoint
- `ObjectMetadataController`: new `GenericUbiquiaDaoController` for `ObjectMetadataEntity` with a `POST /ubiquia/core/flow-service/object-metadata/register/post` sync endpoint
- `FlowMessageController.register()`: `POST /ubiquia/core/flow-service/flow-message/register/post` sync endpoint delegating to `FlowMessageRegistrar.tryRegisterSync()`
- `NetworkDtoMapper`: mapper from `NetworkEntity` to `Network` DTO in `common/java/library/implementation`

### Changed
- `NetworkRepository`: now extends `AbstractEntityRepository<NetworkEntity>` instead of `JpaRepository`/`CrudRepository`, enabling sync-query support
- `AbstractSynchronizationService.sync()`: annotated `@Transactional` to ensure entity reads and remote POSTs share the same transaction context

### Fixed
- `FlowEventDtoMapper`: node ID was not being set on the mapped `Node` stub, causing sync-registered `FlowEvent` DTOs to arrive with a null node reference

## [0.31.0] - 2026-05-21
### Added
- Kubernetes DB sync feature: `ExtraKubernetesSynchronizationService` resolves peer URLs from `ubiquia.cluster.kubernetes.extra.peer-base-urls` for HTTP-based inter-cluster ontology sync without JGroups
- `ExtraKubernetesHeartbeatService`: periodic health probing of extra-cluster peers; tombstones unreachable peers and lifts tombstones on recovery; conditional on `ubiquia.kubernetes.enabled=true`
- `ubiquia_test_kubernetes_db_synch.yaml`: Helm test validating that an ontology bootstrapped into one K8s flow-service agent is propagated to a second agent via `ExtraKubernetesSynchronizationService`

### Fixed
- `ClusterSynchronizationService`: added `initialDelay` equal to the sync frequency on both `@Scheduled` methods to prevent a race condition where the scheduler fired before `ApplicationReadyEvent` created the agent database record
- `ubiquia_test_kubernetes_db_synch.yaml`: replaced `busybox` `nslookup` init container (which treats short names as absolute, bypassing Kubernetes search domains) with a `curlimages/curl` init container that polls `/actuator/health` directly
- `ubiquia_test_simulation_dual_edge_kind_egress.yaml`: added `UBIQUIA_CLUSTER_SYNC_MICROWEIGHT_ENABLED=true` to edge-agent-1 and edge-agent-2 containers; `MicroweightSynchronizationService` was configured with peer URLs but never activated, so ontology sync never ran
- `ubiquia_test_simulation_egress_relay.yaml`: same fix — added `UBIQUIA_CLUSTER_SYNC_MICROWEIGHT_ENABLED=true` to agent-a and agent-b containers

## [0.30.1] - 2026-05-19
### Added
- `ObjectMetadataEntity`: belief-state-owned JPA entity extending `AbstractDomainModelEntity`; replaces dependency on core `org.ubiquia.common.model.ubiquia.entity.ObjectMetadataEntity`
- `ObjectMetadataDto`: DTO in `common/java/model/domain` extending `AbstractDomainModel`; replaces dependency on core `org.ubiquia.common.model.ubiquia.dto.ObjectMetadata`
- `ObjectMetadataEgressDtoMapper`, `ObjectMetadataIngressDtoMapper`, `ObjectMetadataEntityRelationshipBuilder`: belief-state-libraries mappers and relationship builder for the new entity

### Changed
- `ObjectMetadataEntityRepository`: now extends `EntityRepository<ObjectMetadataEntity>` (belief-state entity) instead of core `AbstractEntityRepository<ObjectMetadataEntity>`
- `ObjectMetadataService`: removed all core entity dependencies (`AgentEntity`, `AgentConfig`, `AgentRepository`, `DataSource`); service now only stores file metadata without agent tracking
- `ObjectController.java.template`: updated to extend `AbstractDomainModelController<ObjectMetadataEntity, ObjectMetadataDto>` using belief-state types
- `Application.java.template`: `@EntityScan` and `@EnableJpaRepositories` extended to include `org.ubiquia.common.library.belief.state.libraries` packages so the `ObjectMetadataEntityRepository` bean is registered in generated services
- `core-belief-state-generator-service/build.gradle`: `clean` task now deletes `belief-state-libs/` to ensure stale JARs are not used when recompiling generated code

### Fixed
- Generated belief state services failing to start with `ObjectMetadataEntityRepository` bean not found

## [0.30.0] - 2026-05-18
### Added
- `IntraKubernetesReplicaClusterService`: JGroups KUBE_PING channel for automatic leader election among K8s pod replicas within the same Deployment; non-leaders skip all scheduled tasks (sync, egress relay update, heartbeat)
- `IntraKubernetesHeartbeatService`: periodic HTTP health probing of remote agents registered in this agent's network; tombstones unreachable peers and lifts tombstones on recovery
- `IntraKubernetesSynchronizationService`: resolves HTTP peer URLs for K8s agents by querying the local database for reachable agents with a configured `baseUrl`
- `jgroups-kube.xml`: JGroups stack config using `KUBE_PING` for pod discovery within a Kubernetes namespace
- Helm `serviceaccount-rbac.yaml`: `Role` and `RoleBinding` granting `get`/`list` on `pods` and `endpoints`, required for KUBE_PING
- `ADR_Edge_Cluster_Synchronization.md`: new architecture decision record covering JGroups TCP peer discovery and application-layer HTTP synchronization for microweight edge agents
- `jgroups-kubernetes:2.0.1.Final` dependency in `core-flow-service`

### Changed
- `ClusterSynchronizationService`: `synchronize()` and `tryBuildEgressRelays()` now gate on `IntraKubernetesReplicaClusterService.isLeader()`; when `ubiquia.kubernetes.enabled` is false the service always acts as leader (backwards-compatible for microweight-only deployments)
- Cluster service packages reorganized: `AbstractSynchronizationService`, `AgentSynchronizationService`, `DomainOntologySynchronizationService` moved to `cluster.synchronization.entity`; `MicroweightNetworkManager`, `MicroweightClusterService`, `MicroweightSynchronizationService` moved to `cluster.synchronization.microweight`; Kubernetes services moved to `cluster.synchronization.kubernetes` and renamed with `IntraKubernetes` prefix
- Helm `ubiquia_core_flow_deployment.yaml`: `flowService` replica count now configurable via `ubiquia.components.core.flowService.replicas` (default 1) instead of being hardcoded
- `ADR_NewSQL.md`: added `[2.0.0] - 2026-05-18` status entry marking YugabyteDB deprecated

### Removed
- YugabyteDB removed from the codebase: `jdbc-yugabytedb` driver dropped from all service `build.gradle` files; `wait-for-ysql` init container removed from Helm flow-service deployment; all `h2.enabled`/`yugabyte.enabled` config toggles removed from Helm values and configuration overlays; YugabyteDB Helm templates and documentation removed

## [0.29.0] - 2026-05-15
### Changed
- Node and component proxy URL patterns unified across `core-flow-service` and `core-communication-service`: deployed node endpoints now follow `ubiquia/core-flow-service/{graph}/node/{node}/{endpoint}` and are proxied at `ubiquia/core-communication-service/{graph}/node/{node}/{endpoint}`; deployed component endpoints follow `ubiquia/core-flow-service/{graph}/component/{component}/{endpoint}` and are proxied at `ubiquia/core-communication-service/{graph}/component/{component}/{endpoint}`
- `NodeEndpointRecordBuilder.getBasePathFor()`: base path changed from `graph/{graph}/node/{node}` to `ubiquia/core-flow-service/{graph}/node/{node}`
- `ComponentEndpointRecordBuilder.getBasePathFor()`: base path changed from `/graph/{graph}/component/{component}` to `/ubiquia/core-flow-service/{graph}/component/{component}`
- `DeployedNodeProxyController`: re-mapped from `/ubiquia/core/communication-service/node-reverse-proxy/{nodeName}/**` to `/ubiquia/core-communication-service/{graph}/node/{node}/**`; fixed path-stripping bug (was removing all `/` characters), added hop-by-hop header filtering, connection timeouts, and error-stream forwarding
- `DeployedComponentProxyController`: re-mapped from `/ubiquia/core/communication-service/component-reverse-proxy/{componentName}/**` to `/ubiquia/core-communication-service/{graph}/component/{component}/**`; diagnostics endpoint moved to `GET /ubiquia/core-communication-service/component/get-proxied-urls`
- Helm test `ubiquia_test_communication_service.yaml`: added `GET /ubiquia/core-communication-service/node/get-proxied-urls` and `GET /ubiquia/core-communication-service/component/get-proxied-urls` smoke tests

## [0.28.3] - 2026-05-13
### Fixed
- `FlowEgressRelayTest`: call `relay.teardown()` in `@BeforeEach` to cancel the background scheduler before each test, eliminating the intermittent `ObjectOptimisticLockingFailureException` caused by the 500 ms poll task racing with explicit `tryPollAndForward()` calls to delete the same `FlowMessageEntity`

## [0.28.2] - 2026-05-12
### Added
- Helm test `ubiquia_test_simulation_dual_edge_kind_egress.yaml`: verifies a two-agent edge cluster (microweight, JGroups over localhost) egressing a DAG to a central KIND agent; the verify pod confirms the KIND agent receives exactly one flow event and that its node registry contains both `edge-node-1` and `edge-node-2` via the `E1 → E2 → KIND` sync chain
- `dual-edge-kind-egress-test.yaml` domain ontology: three-node linear DAG (`edge-node-1 → edge-node-2 → kind-node`) used by the dual-edge-kind-egress Helm test

### Fixed
- `ubiquia_test_simulation_cluster_domain_ontology_synch.yaml`: all hook resources (ConfigMaps, Services, Deployments) were missing `hook-succeeded` from `hook-delete-policy`; `microweight-flow-service-{a,b,c,d}` pods and associated resources are now deleted after a successful test run

## [0.28.1] - 2026-05-08
### Added
- Helm test for the multi-hop egress relay chain (`ubiquia_test_simulation_egress_relay.yaml`): deploys three agents (A, B, C), bootstraps the `egress-relay-test` ontology to Agent A, deploys the graph with per-agent cardinality, injects flow messages targeting nodes on B and C, and verifies end-to-end relay delivery

### Changed
- `GraphRegistrar.tryAdaptComponentsToNodes()`: added reverse-direction pass that uses `node.component` from the DTO to establish component↔node links when `component.node` is null (handles synced DTOs received from peer agents)
- All registrars (`GraphRegistrar`, `NodeRegistrar`, `ComponentRegistrar`, `DomainOntologyRegistrar`, `DomainDataContractRegistrar`): incoming DTO `id` is now preserved on the entity so synced records retain the same UUID as the source agent
- `FlowClusterService`: JGroups `bind_addr` is now configurable via `ubiquia.cluster.bind-addr` (default `GLOBAL`); propagated through `jgroups.bind_addr` system property
- `jgroups-tcp.xml`: added `bind_addr` attribute wired to the `jgroups.bind_addr` system property
- `egress-relay-test.yaml`: each component now declares a `node` reference so component↔node links are established on the registering agent
- All Helm test Pods missing `hook-delete-policy` now carry `before-hook-creation,hook-succeeded`; successful test pods are automatically deleted

### Fixed
- `AbstractModelEntity`: implemented `Persistable<String>` with a `@Transient boolean isNew` flag so Spring Data JPA always calls `persist()` for new entities even when an ID is pre-set, preventing spurious `merge()` calls that fail on unsaved FK references

## [0.28.0] - 2026-05-05
### Added
- `FlowEgressRelay`: prototype-scoped component that polls for `FlowMessageEntity` records targeting nodes not locally deployed and forwards them to configured peer agents via `POST /flow-message/receive`; one instance is created per peer by `UbiquiaSynchronizationService`
- `FlowEgressFactory`: factory that instantiates `FlowEgressRelay` prototype beans
- `FlowMessageController`: REST endpoint `POST /ubiquia/core/flow-service/flow-message/receive`; accepts a `FlowMessage` DTO and delegates to `FlowMessageRegistrar`
- `FlowMessageRegistrar`: creates the `FlowEntity` / `FlowEventEntity` / `FlowMessageEntity` chain on receipt of a forwarded flow message
- `NetworkManagementService`: manages `NetworkEntity` agent membership (supports simulation partition scenarios)
- `NetworkRepository`: JPA repository for `NetworkEntity`
- `AbstractSynchronizationService<E, D>`: generic base for typed entity sync services
- `DomainOntologySynchronizationService`: propagates `DomainOntologyEntity` records to peer agents
- `UbiquiaSynchronizationService`: scheduled sync orchestrator (enabled by `ubiquia.cluster.flow-service.sync.enabled=true`); replaces `ModelSynchronizationService`
- `NodeManager.getLocalNodeIds()`: returns the set of node UUIDs currently active in the local node map
- `FlowMessageRepository.findAllByTargetNodeIdNotIn(Collection<String>, Pageable)`: paged query for orphaned flow messages
- `Network` DTO and `NetworkEntity` JPA entity
- `Agent` DTO: added `network` field

### Changed
- `Sync` DTO / `SyncEntity`: `agent` field renamed to `sourceAgent`
- `AgentEntity`: added `network` foreign key; `syncs` `mappedBy` updated to `sourceAgent`
- `FlowMessage` DTO: `targetAdapter` accessors replaced by `targetNode`
- `FlowMessageEntity`: join column renamed from `message_target_adapter_join_id` to `message_target_node_join_id`
- `FlowMessageDtoMapper`: maps `targetNode` (id-only stub) onto the outgoing DTO
- `SyncDtoMapper`: updated to use `sourceAgent`

### Removed
- `ModelSynchronizationService`: superseded by `AbstractSynchronizationService` / `DomainOntologySynchronizationService` / `UbiquiaSynchronizationService`

## [0.27.0] - 2026-04-30
### Added
- UDL domain ontology YAML (`deploy/helm/bootstrap/ontologies/udl.yaml`) added to Helm bootstrap ontologies
- `udl-simulation.yaml` simulation scenario for exercising the UDL domain ontology
- `ScenarioDurationLogicService` in `util-simulation-service`: resolves effective run duration from (1) user-defined `duration` field, (2) last-event offset + 5 s trailing buffer, or (3) a 60 s default; warns when any event offset exceeds a user-supplied duration

### Changed
- `util-simulation-service` service module restructured into `service/builder/`, `service/factory/`, and `service/logic/{pre_processing,post_processing,simulation}/` subdirectories for clearer separation of concerns

## [0.26.0] - 2026-04-24
### Added
- `GraphDeploymentService` in `util-simulation-service`: deploys registered graphs to configured target agents' flow-service instances with retry logic; supports optional `flag`-based node overrides
- `EventDumpService` in `util-simulation-service`: merges fired simulation events with flow-service events fetched from every agent, time-sorts all records, and writes a JSON dump file after each simulation run
- `DagVisualizationService` in `util-simulation-service`: renders an ASCII visualization of every DAG in a domain ontology YAML file
- `graph visualize` CLI command in `util-simulation-service`: renders all graphs in a domain ontology YAML as ASCII DAG diagrams
- `GraphDeploymentInput` and `SemanticVersion` Pydantic models in `util-simulation-service`
- `AgentInput.graph_deployments`: optional list of `GraphDeploymentInput` entries deployed to the target agent before the simulation run
- `Agent.mode` field (defaults to `AgentMode.TEST`)
- `abstract-world-simulation.yaml` simulation scenario and `abstract-world.yaml` domain ontology bootstrap
- `--output-path` and `--output-file-name` options on `simulation run` controlling where the event dump is written

### Changed
- `SimulationService.run()` now returns `list[dict]` of fired event records consumed by `EventDumpService`
- `SimulationService.load()` resolves domain ontology file paths relative to the input YAML's directory so relative paths work regardless of the working directory
- Simulation scenario files reorganized into a `simulations/` subfolder; `simulation.yaml` renamed to `dry-run.yaml`, `three-agent-demo.yaml` removed

### Fixed
- `SimulationEventCommand.execute()` implemented: now POSTs the event payload to the target agent's endpoint via httpx (was previously `NotImplementedError`)
- `GraphRepository`: duplicate-deployment check now includes `graphName`, preventing false conflicts when multiple graphs from the same ontology and version are deployed to the same agent
- `GraphController`: now passes `graphName` to the duplicate check and logs an error on conflict; logs info on successful deployment
- `FlowEventDtoMapper`: removed `FlowDtoMapper` dependency; constructs a lightweight `Flow` stub (id only) when mapping a `FlowEvent`
- `Flow.getModelType()` override added, returning `"Flow"`
- `FlowEvent`: deprecated `getAdapter()`/`setAdapter()` aliases removed

## [0.25.0] - 2026-04-10
### Added
- `PartitionEvent` model in `util-simulation-service`: scenario event that splits agents into named isolated networks at a specified time offset
- `PartitionEventCommand` in `util-simulation-service`: applies a partition by updating the `NetworkTopology` to restrict which agents can communicate
- Helm test `ubiquia_test_simulation_partition.yaml`: verifies that a domain ontology bootstrapped to `partition-flow-service-a` does not propagate to `partition-flow-service-b` after a partition event fires at t=30s
- Helm test `ubiquia_test_simulation_cluster_synch.yaml`: verifies that a domain ontology bootstrapped to one cluster member is propagated to all other members (including a late-joining fourth agent) via JGroups sync

### Changed
- `SimulationInput.AnyEvent` union now includes `PartitionEvent` (discriminated by `type: "partition"`)
- `run_command.py` registers `PartitionEventCommand` under the `"partition"` event type
- `DomainOntologyBootstrapService._MAX_ATTEMPTS` raised from 12 to 24 (240 s retry window) to accommodate CPU-contended simultaneous JVM startup

### Fixed
- `ModelSynchronizationService.trySync()`: replaced `atLeastOnePeerSucceeded` logic with `allPeersSucceeded`; a `SyncEntity` is now only recorded when every peer has successfully acknowledged the sync, preventing peers that are temporarily unavailable at sync time from being permanently orphaned
- Helm test hook-delete-policy on cluster-synch infrastructure resources (ConfigMaps, Services, Deployments) changed from `before-hook-creation,hook-succeeded` to `before-hook-creation`, preventing Helm from deleting them when the simulation-runner pod succeeds before the verify pod runs
- Removed stale `ubiquia_test_devops_simulation.yaml` test template

## [0.24.0] - 2026-04-09
### Added
- `AgentInput.join_offset_time` (`TimeOffset | None`): declares a deferred join time; agents with this field set are excluded from initial setup and provisioned at the specified simulation time offset instead
- `AgentJoinEvent` model in `util-simulation-service`: internal event synthesized from `join_offset_time`; carries the `AgentInput` and is merged into the simulation timeline by `run_command.py`
- `AgentJoinEventCommand` in `util-simulation-service`: provisions the joining agent on demand (TEST mode: connects to existing `base_url`; other modes: delegates to `AgentFactory`) and appends it to the shared live-agent list so clock broadcasts and event routing include it from that point forward
- `SimulationService.extra_events` constructor parameter: additional events merged into the sorted timeline alongside scenario-file events
- Fourth devops-simulation test agent `microweight-flow-service-d` with `join_offset_time: 30s`, verifying that a late-joining node receives the domain ontology via JGroups sync

### Changed
- `SetupService` now skips agents with `join_offset_time` set; deferred agents are logged and provisioned later via `AgentJoinEventCommand`
- `run_command.py` synthesizes `AgentJoinEvent` instances for all deferred agents and registers `AgentJoinEventCommand` with the `EventManager`
- Event models (`Event`, `SimulationEvent`, `AgentJoinEvent`) moved from `model/` into `model/events/` subfolder
- Helm devops-simulation test agents renamed from `ubiquia-agent-{a,b,c}` to `microweight-flow-service-{a,b,c,d}`
- `helm.sh/hook-weight` annotations removed from all devops-simulation test resources

### Fixed
- GitHub Actions `helm test --logs` error caused by Helm attempting to fetch pod logs from ConfigMap hook resources that had already been deleted by `hook-delete-policy: hook-succeeded`

## [0.23.0] 2026-04-08
### Added
- `AgentMode.TEST` in `util-simulation-service`: allows a simulation to target an already-running agent by supplying its `base_url` directly, skipping provisioning
- `BootstrapInput` and `DomainOntologyBootstrapInput` models in `util-simulation-service`: declare domain ontology files and the agents they should be registered on
- `DomainOntologyBootstrapService` in `util-simulation-service`: POSTs domain ontology YAML files to their target agents' registration endpoints with retry logic on connection failures
- `SimulationInput.bootstrap` optional field: wires bootstrap inputs into the simulation pipeline with cross-validation against the declared agents list
- YAML simulation scenario files (`simulation.yaml`, `three-agent-demo.yaml`, `devops-simulation.yaml`) replacing the previous JSON input format
- `DomainOntologyRepository.findByName()` in `core-flow-service`
- `FlowClusterService.rejoinIfSolo()` in `core-flow-service`: scheduled probe that reconnects a sole-member node using randomised jitter to prevent permanent cluster splits during rolling starts
- `ubiquia_test_devops_simulation.yaml` Helm template for the devops simulation test service

### Changed
- `ModelSynchronizationService` in `core-flow-service` now syncs only `DomainOntologyEntity` records to peers; child entities (data contracts, graphs, nodes, components) are created by cascade on registration, removing the need to sync each type individually
- `SetupService` in `util-simulation-service` bypasses the agent builder for TEST-mode agents and constructs the `Agent` directly from `base_url`

## [0.22.0] 2026-04-01
### Added
- `ClockService` in `common/java/library/implementation`: holds a controllable `java.time.Clock`, defaults to `Clock.systemUTC()`, and exposes `setTime(OffsetDateTime)` to fix the clock to a specific GMT instant
- `SimulationController` in each core Java service (`core-flow-service`, `core-communication-service`, `core-belief-state-generator-service`): REST endpoint `POST .../simulation/clock/set` to update the service clock; conditional on `ubiquia.mode != PROD`
- `ubiquia.mode` string property (DEV/TEST/PROD) in Helm `values.yaml`, propagated through all core-service configmaps into each `application.yaml`; replaces the previous top-level `devMode` boolean
- `ClockBroadcastService` in `util-simulation-service`: broadcasts simulated wall-clock time to all core service clock endpoints on every agent before each event is dispatched (when `speed != 1.0`)

## [0.21.0] 2026-03-30
### Added
- JGroups TCP cluster support for `core-flow-service`: `FlowClusterService` manages a peer-to-peer channel and `ModelSynchronizationService` propagates stale model records to cluster peers on a configurable schedule
- `SyncEntity` and `Sync` DTO to track which agents have received each model record
- `SyncDtoMapper`, `SyncRepository`, and `ObjectMetadataRepository`
- `findEntitiesNeedingSync()` query on `AbstractEntityRepository`
- `FlowClusterSyncTestModule` for integration testing of cluster synchronization
- Helm chart and `values.yaml` cluster configuration (`ubiquia.cluster.*`) including sync toggle and seed-host support

## [0.20.0] 2026-03-26
### Added
- `util-simulation-service`: builder pattern for agent/network provisioning (`MicroweightAgentBuilder`, `KindAgentBuilder`, `DockerNetworkBuilder`), `AgentFactory`, `SetupService`, `NetworkService`, `EventManager`, and `SimulationEventCommand` — completing the end-to-end `simulation run` CLI pipeline

## [0.19.0] 2026-03-23
### Added
- Docker Compose bare-metal deployment support (`deploy/compose/`)
- `bare-metal-microweight-install.sh` script to stand up N flow service instances locally without Kubernetes
### Changed
- Flow service bootstrap config split into per-subsystem flags (`bootstrap.belief-state.enabled`, `bootstrap.domain-ontology.enabled`) replacing the single `bootstrap.enabled` flag
- Fixed `application.yaml` YAML structure and config key naming (`flowService` → `flow-service`)
- Flow service default log levels changed from DEBUG to INFO
- Helm flow service configmap: removed redundant `graph.enabled` and `graph.directory` fields

## [0.17.0] 2026-03-11
### Added
- Adding micrometer telemetry to core ubiquia code

## [0.16.1] 2026-03-03
### Fixed
- Null pointer exception. Clearly Java's fault, and not the author.

## [0.16.0] 2026-03-02
### Added
- New Update Entity class to track multi-agent updates

## [0.15.0] 2026-02-03
### Changed
- "ACL" references now Domain
- version & logging stuff

## [0.14.1] 2026-01-26
### Added
- Moar testz

## [0.14.0] 2026-01-13
### Changed
- Lots of stuff with core abstractions.
- AgentCommunicationLanguage has become DomainDataContract
- DomainOntologies are now singular files that contain both DomainDataContract and Graphs 

## [0.13.0] 2025-11-24
### Added
- "Workbench 2.0" IDE stuff

## [0.12.2] 2025-11-04
### Added
- "Workbench 2.0" stuff

## [0.12.1] 2025-10-28
### Fixed
- Fixed generation of embedded models

## [0.12.0] 2025-10-24
### Added
- Ability to infer "embedded" models and handle them accordingly

## [0.11.0] 2025-10-20
### Added
- Workbench Scaffold DAG

## [0.10.0] 2025-10-06
### Changed
- BatchId is now FlowId

## [0.9.0] 2025-10-03
### Added
- Cardinality for DAG deployments

## [0.8.12] 2025-10-02
### Fixed
- Helm test fix

## [0.8.11] 2025-10-01
### Fixed
- Minio image due to deprecation/migration

## [0.8.10] 2025-10-01
### Fixed
- Endpoint of the ACL controller

## [0.8.9] 2025-09-30
### Fixed
- Minio.io uploads work for generated belief states when minio is enabled
- Generated Belief States can run against yugabyte
- PostStartExecCommands order is preserved when running against Yugabyte

## [0.8.8] 2025-09-10
### Added
- Missing communication service endpoints.

## [0.8.7] 2025-09-05
### Fixed
- Validation and exception catching for validation in Flow Service.

## [0.8.5] 2025-09-03
### Fixed
- Devops stuff

## [0.8.3] 2025-09-03
### Fixed
- Devops stuff due to refactored directories

## [0.8.2] 2025-09-03
### Fixed
- Project repo stuff directories

## [0.8.1] 2025-09-03
### Added
- UV Python package builder to devops pipeline

## [0.8.0] 2025-09-03
### Added
- Workbench DAG
### Changed
- Project structure refactoring

## [0.7.0] 2025-08-06
### Added
- Retries into inter-service boostrapping
### Fixed
- Comm service Mono issues

## [0.6.1] 2025-08-04
### Fixed
- Belief state bootstrapping

## [0.6.0] 2025-07-28
### Added
- Belief state bootstrapping
### Changed
- Some refactoring

## [0.5.0] 2025-07-28
### Changed
- Several ACL Model fields to prevent collisions
- Fixed some DAGs

## [0.4.0] 2025-07-11
### Added
- Minio object storage and metadata capability
### Changed
- Refactored some library code

## [0.3.0] 2025-07-10
### Changed
- "Agent" model changed to "Component" and all associated field naming changes

## [0.2.0] 2025-07-03
### Added
- Teardown for belief states
### Fixed
- Teardown now properly works for graph K8s resources

## [0.1.0] - 2025-06-04
### Added
- Initial commit
