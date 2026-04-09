# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
