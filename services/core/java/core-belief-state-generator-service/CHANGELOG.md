# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.34.0] - 2026-05-26
### Fixed
- `BeliefStateOperator.tryDeployBeliefState()`: deployment existence check used `getName().toLowerCase() + getVersion()` (e.g. `"pets1.2.3"`) as the lookup key instead of the canonical Kubernetes name produced by `BeliefStateNameBuilder.getKubernetesBeliefStateNameFrom()` (e.g. `"pets-belief-state-1-2-3"`); injected `BeliefStateNameBuilder` and corrected the check, eliminating intermittent `AlreadyExists` 400 errors when the same domain ontology was deployed more than once

## [0.30.1] - 2026-05-19
### Changed
- `ObjectController.java.template`: updated to extend `AbstractDomainModelController<ObjectMetadataEntity, ObjectMetadataDto>` with belief-state-owned entity and DTO types; removed dependency on core `GenericUbiquiaDaoController`, `ObjectMetadata`, and `ObjectMetadataEntity`
- `Application.java.template`: `@EntityScan` and `@EnableJpaRepositories` now include `org.ubiquia.common.library.belief.state.libraries.entity` and `org.ubiquia.common.library.belief.state.libraries.repository` so the `ObjectMetadataEntityRepository` bean is registered in generated services

### Fixed
- `build.gradle`: `clean` task now deletes `belief-state-libs/` directory, preventing stale JARs from being used when compiling generated code after a rebuild

## [0.30.0] - 2026-05-18
### Changed
- `BeliefStateGenerationSupportProcessor`: `resolveDbTokens()` (formerly `resolveDbTokensFromBooleans()`) now unconditionally returns H2 embedded database config; `h2.enabled` and `yugabyte.enabled` conditional logic removed

### Removed
- `jdbc-yugabytedb` runtime dependency
- `@Value("${ubiquia.agent.database.h2.enabled:false}")` and `@Value("${ubiquia.agent.database.yugabyte.enabled:false}")` fields from `BeliefStateGenerationSupportProcessor`

## [0.22.0] 2026-04-01
### Added
- `SimulationController`: REST endpoint `POST /ubiquia/core/belief-state-generator-service/simulation/clock/set` to update the service clock via `ClockService`; conditional on `ubiquia.mode != PROD`
- `ubiquia.mode` property in `application.yaml` (default `PROD`), propagated from Helm configmap

## [0.18.0] 2026-03-18
### Changed
- Micrometer version now managed by Spring Boot BOM

## [0.12.1] 2025-10-28
### Fixed
- Fixed generation of embedded models

## [0.12.0] 2025-10-24
### Added
- Ability to infer "embedded" models and handle them accordingly

## [0.8.12] 2025-10-01
### Fixed
- Fixed an endpoint

## [0.4.0] 2025-07-03
### Added
- Minio services and minio injection for generated belief states 

## [0.2.0] 2025-07-03
### Added
- Teardown for belief states
### Fixed
- Teardown now properly works for graph K8s resources

## [0.1.0] 2025-05-21
### Added
- Initial commit
