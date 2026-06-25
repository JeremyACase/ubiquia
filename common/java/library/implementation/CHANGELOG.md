# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.39.0] - 2026-06-25
### Changed
- `NodeEndpointRecordBuilder`: updated to use `node.getTargetComponent()` (renamed from `getComponent()`)
- `NodeDtoMapper`: updated both `map()` overloads to use `getTargetComponent()` / `setTargetComponent()` (renamed from `getComponent()` / `setComponent()`)

## [0.33.0] - 2026-05-25
### Changed
- `GenericDtoMapper.setAbstractEntityFields()`: removed `SyncDtoMapper` injection and `setSyncs()` call; the `syncs` field on `AbstractModel` is no longer populated during entity egress to avoid triggering an unbounded lazy-load; query `GET /sync/query/params?sourceAgent.id=<uuid>` instead

## [0.32.0] - 2026-05-22
### Added
- `NetworkDtoMapper`: maps `NetworkEntity` to `Network` DTO

### Fixed
- `FlowEventDtoMapper`: node ID is now set on the mapped `Node` stub so sync-registered `FlowEvent` records carry a resolvable node reference

## [0.29.0] - 2026-05-15
### Changed
- `NodeEndpointRecordBuilder.getBasePathFor()`: path changed from `graph/{graph}/node/{node}` to `ubiquia/core-flow-service/{graph}/node/{node}`; all dynamically-registered node endpoints (`/push`, `/back-pressure`, `/queue/peek`, `/queue/pop`) now live under the new prefix
- `ComponentEndpointRecordBuilder.getBasePathFor()`: path changed from `/graph/{graph}/component/{component}` to `/ubiquia/core-flow-service/{graph}/component/{component}`

## [0.28.0] - 2026-05-05
### Changed
- `FlowMessageDtoMapper`: now maps `targetNode` (id-only stub) onto the outgoing `FlowMessage` DTO so egress relay forwarding carries the correct node reference
- `SyncDtoMapper`: updated to use renamed `sourceAgent` field (was `agent`) on `SyncEntity`

## [0.26.0] - 2026-04-24
### Fixed
- `FlowEventDtoMapper`: removed `FlowDtoMapper` dependency; now constructs a lightweight `Flow` stub (id only) when mapping a `FlowEvent`, avoiding unnecessary eagerly-loaded joins

## [0.22.0] 2026-04-01
### Added
- `ClockService`: Spring `@Service` that holds a `volatile Clock` (defaults to `Clock.systemUTC()`), with `setTime(OffsetDateTime)` to fix it to a specific GMT instant, `getClock()`, and `getCurrentTime()`

## [0.21.0] 2026-03-30
### Added
- `SyncDtoMapper` for mapping `SyncEntity` to `Sync` DTO

## [0.18.0] 2026-03-18
### Changed
- Micrometer version now managed by Spring Boot BOM

## [0.1.0] 2025-06-02
### Added
- Initial commit
