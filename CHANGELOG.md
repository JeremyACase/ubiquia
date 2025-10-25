# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
