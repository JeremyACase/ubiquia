# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.19.0] 2026-05-19
### Added
- `ObjectMetadataEntity`: new JPA entity in `entity` package extending `AbstractDomainModelEntity`; annotated with `@Entity(name = "BeliefStateObjectMetadataEntity")` and `@Table(name = "belief_state_object_metadata")` to avoid collision with the identically-named core entity
- `ObjectMetadataEgressDtoMapper`: reflection-based egress mapper from `ObjectMetadataEntity` to `ObjectMetadataDto`
- `ObjectMetadataIngressDtoMapper`: ingress mapper that bypasses the generated-class lookup in the parent (`org.ubiquia.domain.generated.*`) since this entity is not generated
- `ObjectMetadataEntityRelationshipBuilder`: no-op relationship builder for `ObjectMetadataEntity` (entity has no domain entity relationships)

### Changed
- `ObjectMetadataEntityRepository`: now extends `EntityRepository<ObjectMetadataEntity>` (the new belief-state entity) instead of `AbstractEntityRepository` from the core library
- `ObjectMetadataService`: removed all core entity dependencies (`AgentEntity`, `AgentConfig`, `AgentRepository`, `DataSource`); now persists file metadata directly against the new entity and returns `ObjectMetadataDto`

## [0.18.0] 2026-03-18
### Added
- Controller and model tags to MicroMeter metrics
### Fixed
- Prometheus endpoint conditional property key

## [0.12.0] 2025-10-24
### Added
- Ability to infer "embedded" models and handle them accordingly

## [0.1.0] 2025-06-02
### Added
- Initial commit
