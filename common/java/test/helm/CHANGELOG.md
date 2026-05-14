# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
