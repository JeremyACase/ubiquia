# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.38.1] - 2026-06-17
### Fixed
- Resolved all Google Java Style checkstyle violations across main sources: missing Javadoc on types and methods, `CustomImportOrder`, `AbbreviationAsWordInName`, `LineLength`, `OperatorWrap`, `SummaryJavadoc`, `EmptyLineSeparator`

## [0.36.0] - 2026-06-10
### Removed
- `simulateOutputPayload` field and its getter/setter from `NodeSettings`; simulated-output behavior is now driven by `NodeType.HIDDEN` and `ComponentType.TEMPLATE` in the flow service

## [0.31.0] - 2026-05-21
### Added
- `AgentEntity.baseUrl`: nullable column storing the HTTP base URL of the agent; populated during initialization from `AgentConfig` and used by heartbeat and sync services for peer discovery
- `AgentEntity.reachable`: non-null boolean column (default `true`) managed by `IntraKubernetesHeartbeatService`; unreachable agents are excluded from sync peer resolution

## [0.28.1] - 2026-05-08
### Fixed
- `AbstractModelEntity`: implemented `Persistable<String>` (`@Transient boolean isNew = true`, `@PrePersist generateId()`, `@PostPersist @PostLoad markPersisted()`) so Spring Data JPA always issues `persist()` for new entities even when their ID is pre-set, preventing `merge()` calls that fail on unsaved FK references

## [0.28.0] - 2026-05-05
### Added
- `Network` DTO: model representing a named network of agents, with `List<Agent> agents`
- `NetworkEntity`: JPA entity for network membership with `@OneToMany List<AgentEntity> agents`
- `Agent` DTO: added `network: Network` field

### Changed
- `Sync` DTO: renamed `agent` field to `sourceAgent` (getter/setter updated accordingly)
- `SyncEntity`: renamed `agent` field to `sourceAgent`; updated `@OneToMany mappedBy` in `AgentEntity` from `"agent"` to `"sourceAgent"`
- `AgentEntity`: added `@ManyToOne NetworkEntity network` field; updated `syncs` collection `mappedBy` to `"sourceAgent"`
- `FlowMessage` DTO: replaced deprecated `getTargetAdapter()`/`setTargetAdapter()` with `getTargetNode()`/`setTargetNode()`
- `FlowMessageEntity`: renamed join column from `message_target_adapter_join_id` to `message_target_node_join_id`

## [0.26.0] - 2026-04-24
### Fixed
- `Flow`: added `getModelType()` override returning `"Flow"`
- `FlowEvent`: removed deprecated `getAdapter()`/`setAdapter()` aliases (use `getNode()`/`setNode()` directly)

## [0.21.0] 2026-03-30
### Added
- `SyncEntity` to track which agents have received a given model record
- `Sync` DTO counterpart for `SyncEntity`

## [0.16.1] 2026-03-03
### Fixed
- Null pointer exception. Clearly Java's fault, and not the author.

## [0.16.0] 2026-03-02
### Added
- New Update Entity class to track multi-agent updates

## [0.8.9] 2025-09-30
### Fixed
- Order is preserved for ComponentEntity ExecCommands

## [0.1.0] 2025-05-21
### Added
- Initial commit
