# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
