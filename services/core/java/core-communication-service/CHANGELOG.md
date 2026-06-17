# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.38.0] - 2026-06-16
### Added
- `HttpProxyConnectionBuilder`: fluent Builder for `HttpURLConnection`; encapsulates method, header-copy, body-stream, connect/read timeout, and `Accept-Encoding: identity` setup; replaces ad-hoc connection setup scattered across `DeployedNodeProxyController` and `DeployedComponentProxyController`
- `ProxyResponseRewriter`: `@Service` for HTML/CSS body rewriting; injects/overrides `<base href>`, rewrites root-absolute and relative `src`/`href` asset URLs in HTML, and rewrites `url(/)` / `@import "/"` in CSS; charset detection and Content-Type patching included

### Changed
- `AbstractReverseProxyController`: introduced as Template Method base for all servlet reverse-proxy controllers; `executeProxy()` drives the proxy lifecycle and delegates `resolveUpstream()` to subclasses; `UpstreamResolution` protected record with `public static` factory methods centralises upstream-URI decisions
- `AbstractUbiquiaDaoControllerProxy<T>`: removed `extends AbstractModel` type bound so `Agent` can be used as the type parameter; `webClient` field changed from `private` to `protected`
- `AgentControllerProxy`: now extends `AbstractUbiquiaDaoControllerProxy<Agent>`; Javadoc added; long `.onErrorResume(...)` chain split to stay within line-length limit
- `GraphControllerProxy`: removed shadowed `@Autowired private WebClient webClient` (now inherited as `protected`); Javadoc added for proxy methods
- `DeployedComponentProxyController`: `buildServiceBase()` simplified to use `flowServiceConfig.getBaseUrl()`; removed defensive scheme-check and `Locale` import
- `DeployedNodeProxyController`: `buildServiceBase()` uses `flowServiceConfig.getBaseUrl()`
- `DashboardProxyController`: uses `dashboardServiceConfig.getBaseUrl()` instead of separate `getUrl()` + `port(getPort())` calls
- `DashboardServiceConfig`: added `getBaseUrl()` returning `url + ":" + port`
- `ComponentProxyManager`: removed unused `WebClient` field; fixed double-lookup anti-pattern (`containsKey` + `get` → single `get` with null check)
- `NodeProxyManager`: removed unused `WebClient` and `FlowServiceConfig` fields; fixed double-lookup in `getRegisteredEndpointForNodeName()`; for-each now iterates `.values()` directly
- `DeployedGraphPoller`: uses `flowServiceConfig.getBaseUrl()` in both polling methods; import ordering corrected to alphabetical

### Fixed
- Resolved all Google Java Style checkstyle warnings across modified files: `LineLength`, `MissingJavadocMethod`, `MissingJavadocType`, `OperatorWrap`, `SeparatorWrap`, `CustomImportOrder`, and `MethodName` violations

## [0.36.0] - 2026-06-10
### Added
- `DashboardServiceConfig` for binding dashboard service URL and port from application config
- `DashboardProxyController` for proxying requests to the Ubiquia Dashboard service

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
