# Changelog

All notable changes to `util-simulation-service` will be documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning is inherited from the root Ubiquia project.

## [0.19.0] - 2026-03-24

### Added
- Initial implementation of `SimulationService` — reads and validates a JSON simulation input file, iterates events, and dispatches each via `_post_event` (stub).
- Initial implementation of `AnalysisService` — paginates through a flow-service REST API to collect flow events.
- Pydantic models: `SimulationInput`, `Event`, `Network`, `TimeOffset`, `TimeUnit`, `AgentMode`.
- `SimulationInput` model validator ensuring all agents referenced in networks are declared in the top-level agents list.
- CLI entry point (`util-simulation-service simulate`) using Click with `--mode` and `--input-file` options.
- Unit tests for all models and services (27 tests via pytest).
- Gradle `testPython` task wired to the `check` lifecycle; wheel version stamped from `UBIQUIA_VERSION` env var at build time.
