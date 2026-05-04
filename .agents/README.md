# .agents Reality Sync

This directory mirrors canonical AI workflow standards for tooling that expects a `.agents` root.

Primary source of truth remains `.agent/`.

## Canonical Rules Snapshot

- Backend: Ktor (`backend-ktor`)
- Sidecar: Node.js + AgentKit (`agentkit-sidecar`)
- Backend-sidecar communication: internal HTTP + `x-sidecar-secret`
- Frontend: Compose Multiplatform + Unidirectional Data Flow
- Local data: offline-first repositories with SQLDelight-backed persistence
- Error handling: frontend domain errors must match backend machine error codes exactly
- Build execution: serial only, with `--no-daemon --no-configuration-cache` on every Gradle command
- Commit policy: no `TODO`, no mock data, no placeholder logic
