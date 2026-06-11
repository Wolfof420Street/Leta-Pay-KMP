# Dependency Inventory

This document catalogues the key dependencies used in the Leta Pay project, flagging any that are outdated or risky.

## Core Frameworks
| Dependency | Version | Purpose |
|------------|---------|---------|
| Kotlin | 2.2.21 | Language & Compiler |
| Coroutines | 1.10.2 | Asynchronous programming |
| Serialization | 1.9.0 | JSON Parsing |
| Ktor | 3.3.3 | Networking & Server |
| Koin | 4.1.1 | Dependency Injection |
| SQLDelight | 2.0.2 | Shared Database |

## UI & Multiplatform
| Dependency | Version | Purpose |
|------------|---------|---------|
| Compose Multiplatform | 1.9.3 | UI Framework |
| Coil | 3.3.0 | Image Loading |
| Navigation Compose | 2.9.1 | Shared Navigation |
| Multiplatform Settings | 1.3.0 | Key-Value Storage |
| Kermit | 2.0.8 | Logging |

## Backend specific
| Dependency | Version | Purpose |
|------------|---------|---------|
| Exposed | 0.55.0 | Database ORM |
| PostgreSQL | 42.7.4 | Database Driver |
| Argon2 | 2.11 | Password Hashing (if used) |
| Web3j | 4.12.2 | Ethereum integration |
| Java JWT | 4.4.0 | JWT Handling |

## Risk Assessment

### Outdated Dependencies
- **Kotlin 2.2.21**: ⚠️ **Critical Check**. Kotlin 2.1.0 is the latest stable. 2.2.x is likely a development or typo version in this template. Needs verification.
- **Ktor 3.3.3**: Latest stable is likely lower (3.0.x is current stable as of late 2024). Verify if using pre-release.

### Security Flags
- **Web3j**: Ensure use of latest security patches for crypto operations.
- **Argon2**: High resource consumption, check server scaling if used for high-frequency operations.

## Development Tools
- **Detekt**: 1.23.8 (Static Analysis)
- **Spotless**: 7.0.2 (Formatting)
- **Dependency Guard**: 0.5.0 (Vulnerability checking)
