# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.38.9] - 2026-06-23
### Fixed
- Resolved all checkstyle linting warnings: added missing Javadoc comments on all public types and
  non-property methods across `component/`, `interfaces/`, and `service/`; wrapped long `logger`
  field declarations to stay within the 100-character line limit.

## [0.35.0] - 2026-05-27
### Added
- `integration/yugabyte/ubiquia_test_heavyweight_yugabyte.yaml`: Helm test for the YugabyteDB heavyweight deployment tier; waits for core-flow-service to become healthy, then asserts the `/actuator/health` response contains `"PostgreSQL"` (YugabyteDB YSQL) and `"status":"UP"`; gated by `testing.yugabyte.enabled`

### Changed
- Helm test agent and service names updated across all integration tests to include the test name as a prefix, making spawned resources identifiable by which test created them

## [0.31.0] - 2026-05-21
### Added
- `ubiquia_test_kubernetes_db_synch.yaml`: new Helm test; two independent K8s flow-service Deployments (`k8s-flow-service-a`, `k8s-flow-service-b`) with a shared H2 database; asserts that the `pets` ontology bootstrapped into service-a is propagated to service-b via `ExtraKubernetesSynchronizationService` within the sync window
- `ubiquia-test-k8s-db-synch-config` ConfigMap: shared application config for both agents (H2 in-memory, sync enabled, `ubiquia.kubernetes.enabled=false`)
- `ubiquia-test-k8s-db-synch-ontologies` ConfigMap: mounts `pets.yaml` into service-a for ontology bootstrapping

### Fixed
- `ubiquia_test_kubernetes_db_synch.yaml`: replaced `busybox` `nslookup` init container with `curlimages/curl` health-check loop; `nslookup` in busybox treats short hostnames as absolute (no search domain appended), causing permanent DNS failures in Kubernetes
- `ubiquia_test_simulation_dual_edge_kind_egress.yaml`: added `UBIQUIA_CLUSTER_SYNC_MICROWEIGHT_ENABLED=true` to `flow-service-edge-1` and `flow-service-edge-2` containers; `MicroweightSynchronizationService` (and its peer URL resolution) is gated on this property — without it, `UBIQUIA_CLUSTER_SYNC_PEER_BASE_URLS` had no effect and ontology sync never ran
- `ubiquia_test_simulation_egress_relay.yaml`: same fix — added `UBIQUIA_CLUSTER_SYNC_MICROWEIGHT_ENABLED=true` to `flow-service-a` and `flow-service-b` containers

## [0.28.2] - 2026-05-12
### Added
- `ubiquia_test_simulation_dual_edge_kind_egress.yaml`: two microweight edge agents sharing a pod form a JGroups cluster and relay a DAG message hop-by-hop to a central KIND agent (separate Deployment); the verify pod asserts the KIND agent received exactly one flow event and holds node registry entries for both edge nodes synced via the `E1 → E2 → KIND` peer chain
- `dual-edge-kind-egress-test.yaml`: domain ontology backing the dual-edge-kind-egress test; three-node DAG with `edge-node-1`, `edge-node-2`, and `kind-node`

### Fixed
- `ubiquia_test_simulation_cluster_domain_ontology_synch.yaml`: `hook-delete-policy` was `before-hook-creation` only on all non-verify hook resources; all resources now carry `before-hook-creation,hook-succeeded` so `microweight-flow-service-{a,b,c,d}` Deployments, Services, and ConfigMaps are cleaned up after a successful test run

## [0.27.0] 2026-04-30
### Added
- UDL domain ontology YAML added to Helm bootstrap ontologies directory

## [0.15.0] 2026-02-03
### Changed
- "ACL" references now Domain

## [0.1.0] 2025-07-14
### Added
- Initial commit
