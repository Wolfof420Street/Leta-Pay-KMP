# Frontend Agent - Tech Stack Reference

## Canonical Frontend Stack

- **Framework**: Compose Multiplatform
- **Language**: Kotlin Multiplatform
- **Targets**: Android, iOS, Desktop JVM, Web JS
- **State model**: Unidirectional Data Flow + StateFlow-driven UI state
- **Local persistence**: SQLDelight repositories (offline-first)

## Contract and Error Rules

- Frontend domain errors must map one-to-one with backend machine error codes
- Build/sign/broadcast UX must preserve backend policy semantics
- Web token handling must respect non-persistent session constraints

## Project Structure Focus

- Shared logic in `feature/*`, `core/*`, and `cmp-shared/`
- Platform UI entry points in `cmp-android/`, `cmp-ios/`, `cmp-desktop/`, `cmp-web/`
- Platform-specific implementations remain in platform source sets only

## Command Baseline

- `./gradlew --no-daemon --no-configuration-cache :cmp-web:compileKotlinJs`
- `./gradlew --no-daemon --no-configuration-cache :cmp-web:jsBrowserDistribution`

## Commit Hygiene

- Do not commit `TODO`, `FIXME`, mock payloads, or placeholder data
