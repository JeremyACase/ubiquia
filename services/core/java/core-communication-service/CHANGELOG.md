# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.29.0] - 2026-05-15
### Changed
- `DeployedNodeProxyController`: base mapping changed to `/ubiquia/core-communication-service`; route changed from `/{nodeName}/**` to `/{graph}/node/{node}/**`; target URL now built directly from path variables as `ubiquia/core-flow-service/{graph}/node/{node}/{tail}`; fixed path-stripping bug (was removing all `/` characters from the trailing path); added hop-by-hop header filtering, 10 s connect / 60 s read timeouts, and error-stream forwarding for 4xx/5xx responses; diagnostics endpoint moved to `GET /ubiquia/core-communication-service/node/get-proxied-urls`
- `DeployedComponentProxyController`: base mapping changed to `/ubiquia/core-communication-service`; route changed from `/{componentName}/**` to `/{graph}/component/{component}/**`; `graph` path variable added to `proxyToComponent()`; diagnostics endpoint moved to `GET /ubiquia/core-communication-service/component/get-proxied-urls`

## [0.22.0] 2026-04-01
### Added
- `SimulationController`: REST endpoint `POST /ubiquia/core/communication-service/simulation/clock/set` to update the service clock via `ClockService`; conditional on `ubiquia.mode != PROD`
- `ubiquia.mode` property in `application.yaml` (default `PROD`), propagated from Helm configmap

## [0.18.0] 2026-03-18
### Changed
- Micrometer version now managed by Spring Boot BOM

## [0.8.8] 2025-09-10
### Added
- Missing comm service endpoints

## [0.7.0] 2025-08-06
### Fixed
- Mono serialization issues

## [0.1.0] 2025-05-21
### Added
- Initial commit
