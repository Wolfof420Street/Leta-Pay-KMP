# Leta Pay AI System Instructions (Phase 15)

These instructions are mandatory for any agent operating on this repository.

## 1. Backend Rules

- Backend runtime is Kotlin Ktor.
- Sidecar runtime is Node.js + Coinbase AgentKit.
- Ktor and sidecar communicate via internal HTTP only.
- Sidecar endpoints must require and validate `x-sidecar-secret`.
- Sidecar must never be exposed as a public internet service.

## 2. Frontend Rules

- UI is Compose Multiplatform.
- State updates follow Unidirectional Data Flow.
- Repositories must remain offline-first with SQLDelight-backed local persistence.
- Frontend domain errors must match backend machine errors exactly.

## 3. Build Rules

- Run build and validation tasks serially.
- Every Gradle command must include:
  - `--no-daemon`
  - `--no-configuration-cache`

## 4. Zero Placeholder Rule

- Never commit `TODO`, `FIXME`, mock data, or placeholder API responses.
- If blocked, report blocker explicitly instead of stubbing behavior.
