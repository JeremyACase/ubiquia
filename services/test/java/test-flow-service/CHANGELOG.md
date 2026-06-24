# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.38.12] - 2026-06-23
### Fixed
- Resolved checkstyle warnings: added missing Javadoc comments to `Application`, `Config`,
  `ComponentDeploymentTestModule`, `ComponentTeardownTestModule`, and `TestManager`; added
  Javadoc to `Config.init()` and `TestManager.registerAndRunTests()`; fixed import ordering
  and missing blank line before class in `FlowClusterSyncTestModule`; wrapped overlong logger
  line in `ComponentDeploymentTestModule`.

## [0.21.1] 2026-06-18
### Fixed
- Resolved checkstyle linting warnings in `common/java/library/api`: import ordering in `MinioConfig`, overlong Javadoc comment in `AgentRepository`, and suppressed unavoidable `LineLength` violation on Spring Data derived query method name

## [0.21.0] 2026-03-30
### Added
- `FlowClusterSyncTestModule` for integration testing of JGroups cluster synchronization

## [0.8.0] 2025-07-12
### Added
- Initial commit
