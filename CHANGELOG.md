# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.38.12] - 2026-06-23

### Fixed

- Resolved checkstyle warnings in `services/test/java/test-flow-service`: added missing Javadoc
  comments to `Application`, `Config`, `ComponentDeploymentTestModule`,
  `ComponentTeardownTestModule`, and `TestManager`; added Javadoc to the `Config.init()` and
  `TestManager.registerAndRunTests()` methods; fixed import ordering and blank line in
  `FlowClusterSyncTestModule`; wrapped an over-length logger line in
  `ComponentDeploymentTestModule`.

## [0.38.7] - 2026-06-22

### Fixed

- Linting corrections in `common/java/model/domain`
