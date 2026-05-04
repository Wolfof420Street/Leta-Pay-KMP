# Leta Pay Documentation Index

This directory contains canonical architecture and runtime documentation for the current Leta Pay stack.

## Canonical Docs

- `ARCHITECTURE.md`: system topology, trust boundaries, sidecar contract, kill switch and idempotency boundaries
- `API_REFERENCE.md`: backend endpoint contract and machine error catalog
- `ENVIRONMENT.md`: required and optional environment variables for backend, sidecar, and compose
- `LOCAL_DEV.md`: local runbook for Docker and KMP targets
- `USER_FLOWS.md`: auth, build-sign-broadcast, and operational safety paths
- `DATA_MODEL.md`: backend data model and idempotency persistence design

## Deployment

Use root-level `deployment.md` for VPS self-hosting procedures and `docker-compose.prod.yml` for production orchestration.

## Scope Note

Template-era docs for actionhub workflows, template sync scripts, and managed deployment playbooks were removed in Phase 15 reality sync.
