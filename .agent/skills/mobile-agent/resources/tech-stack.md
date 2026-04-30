# Mobile Agent - Tech Stack Reference

## Kotlin Multiplatform (Primary)
- Framework: Kotlin Multiplatform + Compose Multiplatform
- Shared logic: cmp-shared and core modules
- Platform targets: Android and iOS (plus shared compatibility with desktop/web decisions)
- State: StateFlow and ViewModel patterns
- Networking: Ktor Client
- Persistence: project-selected local storage abstractions in shared/data modules
- Testing: commonTest with platform-specific coverage where behavior diverges

## Module Placement

1. Shared domain/data logic in shared/core modules
2. Platform adapters in platform modules only
3. UI layer per target with shared contracts
4. expect/actual only where true platform divergence exists

## Architecture Pattern

Use case -> repository interface -> repository implementation -> platform/data adapters

## Platform Guidelines

- Android follows Material guidance and platform lifecycle constraints
- iOS follows HIG and native lifecycle constraints
- Keep platform-specific code minimal and explicit
