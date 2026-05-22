# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.31.0] - 2026-05-21
### Added
- `AgentRepository.findByNetworkAndBaseUrlIsNotNull(NetworkEntity)`: returns all agents in a network that have a `baseUrl` set; used by `IntraKubernetesHeartbeatService` for health probing
- `AgentRepository.findByNetworkAndBaseUrlIsNotNullAndReachableIsTrue(NetworkEntity)`: returns reachable agents in a network with a `baseUrl`; used by `IntraKubernetesSynchronizationService` for peer URL resolution

## [0.21.0] 2026-03-30
### Added
- `findEntitiesNeedingSync()` query to `AbstractEntityRepository` to identify records with no sync history or with updates newer than their last sync

## [0.1.0] 2025-06-02
### Added
- Initial commit
