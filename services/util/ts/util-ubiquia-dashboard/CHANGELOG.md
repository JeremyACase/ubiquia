# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.36.0] 2026-06-10
### Added
- Initial commit of the Ubiquia Dashboard — a React/TypeScript web UI for monitoring and managing Ubiquia deployments
- Panels for Agents, Belief States, Components, DAGs, Flow Tracing, and Ontologies
- Sidebar navigation and shared layout
- API client layer for flows, graphs, components, agents, belief states, and ontologies
- Dockerfile and nginx config for containerized deployment
- Helm templates and values for deploying the dashboard alongside the rest of the Ubiquia stack
