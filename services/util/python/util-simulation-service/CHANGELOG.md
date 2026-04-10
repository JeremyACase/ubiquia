# Changelog

All notable changes to `util-simulation-service` will be documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning is inherited from the root Ubiquia project.

## [0.24.0] - 2026-04-09
### Added
- `AgentInput.join_offset_time` (`TimeOffset | None`): when set, the agent is excluded from initial provisioning and joined to the simulation at the specified time offset
- `AgentJoinEvent` model (`model/events/agent_join_event.py`): internal event type synthesized by `run_command.py` from agents with `join_offset_time`; not part of the YAML scenario `events` union
- `AgentJoinEventCommand`: `EventCommand` that provisions the agent on demand (TEST mode uses `base_url` directly; other modes delegate to `AgentFactory`) and appends it to the shared live-agent list
- `SimulationService` accepts an `extra_events` parameter; these are merged with scenario-file events into the sorted dispatch timeline

### Changed
- `SetupService.run()` now filters out agents with `join_offset_time` set; only immediately-active agents are returned
- `run_command.py` synthesizes `AgentJoinEvent` instances for deferred agents and registers `AgentJoinEventCommand` under the `"agent_join"` event type
- Event models reorganized: `Event`, `SimulationEvent`, and `AgentJoinEvent` moved from `model/` into `model/events/`

## [0.23.0] - 2026-04-08
### Added
- `AgentMode.TEST`: targets an already-running agent; requires `base_url` on the `AgentInput` (enforced by model validator)
- `AgentInput.base_url` optional field with validator: required when `mode == TEST`, rejected otherwise
- `BootstrapInput` model: aggregates a list of `DomainOntologyBootstrapInput` entries
- `DomainOntologyBootstrapInput` model: pairs a YAML ontology `file` path with a list of target agent names
- `SimulationInput.bootstrap` optional field: wires bootstrap config into the simulation; validates that all target names are declared in the agents list
- `DomainOntologyBootstrapService`: reads each ontology YAML and POSTs it to each target agent's `/ubiquia/core/flow-service/domain-ontology/register/post` endpoint; retries up to 12 times on `ConnectError` with a 10 s interval
- YAML simulation scenario files (`simulation.yaml`, `three-agent-demo.yaml`, `devops-simulation.yaml`)

### Changed
- `SetupService`: TEST-mode agents bypass the agent builder — an `Agent` is constructed directly from `AgentInput.base_url`
- `run_command.py` updated to instantiate `DomainOntologyBootstrapService` and invoke it before the simulation run when `bootstrap` is present

## [0.22.0] - 2026-04-01
### Added
- `ClockBroadcastService`: broadcasts simulated wall-clock time (`start_time + event.time_offset`) to all 3 core service `SimulationController` endpoints on every agent before each event is dispatched, when `speed != 1.0`
- `SimulationService` now accepts an optional `ClockBroadcastService` and calls it before each dispatch when running at non-real-time speed
- `run_command.py` wires `ClockBroadcastService` into `SimulationService` automatically

## [0.20.0] - 2026-03-26

### Added
- Builder pattern for agent provisioning: abstract `AgentBuilder` and `NetworkBuilder` bases, `MicroweightAgentBuilder` (builds and starts a Docker container running `core-flow-service`), `KindAgentBuilder`, and `DockerNetworkBuilder`.
- `AgentFactory` — resolves the correct `AgentBuilder` for a given `AgentMode`.
- `SetupService` — provisions all agents declared in a `SimulationInput` by delegating to `AgentFactory`.
- `NetworkService` — connects provisioned agents into declared networks via `NetworkBuilder`; falls back to a single default network if none are specified; returns a `NetworkTopology`.
- `EventManager` — command-pattern invoker that dispatches each `Event` to its registered `EventCommand` by type discriminator.
- `SimulationEventCommand` — `EventCommand` stub that validates the event type and raises `NotImplementedError`.
- `NetworkTopology` model — tracks peer adjacency across networks.
- `AgentInput` model — carries per-agent name/mode configuration from the simulation input file.
- `simulation run` CLI subcommand (replacing the flat `simulate` command) — wires together `SetupService`, `NetworkService`, `SimulationService`, and `AnalysisService` end-to-end.
### Changed
- `SimulationService` now dispatches events through `EventManager` rather than a direct `_post_event` stub.
- CLI restructured from a single `simulate` command to a `simulation` group with a `run` subcommand; `--mode` flag removed (mode is now per-agent in the input file).

## [0.19.0] - 2026-03-24

### Added
- Initial implementation of `SimulationService` — reads and validates a JSON simulation input file, iterates events, and dispatches each via `_post_event` (stub).
- Initial implementation of `AnalysisService` — paginates through a flow-service REST API to collect flow events.
- Pydantic models: `SimulationInput`, `Event`, `Network`, `TimeOffset`, `TimeUnit`, `AgentMode`.
- `SimulationInput` model validator ensuring all agents referenced in networks are declared in the top-level agents list.
- CLI entry point (`util-simulation-service simulate`) using Click with `--mode` and `--input-file` options.
- Unit tests for all models and services (27 tests via pytest).
- Gradle `testPython` task wired to the `check` lifecycle; wheel version stamped from `UBIQUIA_VERSION` env var at build time.
