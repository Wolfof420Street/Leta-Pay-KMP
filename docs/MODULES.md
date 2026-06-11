# Project Modules

This document describes the responsibility and dependency structure of the modules in the Leta Pay project.

## Module Graph Overview

The project follows a layered architecture with platform-specific entry points, shared UI/Logic, and modularized core/feature layers.

### App Entry Points (Platform Specific)
- **`cmp-android`**: Android application module. Depends on `cmp-shared` and `cmp-navigation`.
- **`cmp-ios`**: iOS Xcode project (Swift). Depends on `cmp-shared` via CocoaPods.
- **`cmp-desktop`**: Desktop JVM application. Depends on `cmp-shared` and `cmp-navigation`.
- **`cmp-web`**: Web application (Kotlin/JS). Depends on `cmp-shared` and `cmp-navigation`.
- **`backend-ktor`**: Ktor server providing API for all clients.

### Shared UI & Navigation
- **`cmp-shared`**: Main entry point for Compose Multiplatform UI. Contains common screens and business logic.
- **`cmp-navigation`**: Shared navigation logic and route definitions for all platforms.

### Feature Modules (`feature:*`)
Modular features used by `cmp-shared`.
- **`feature:auth`**: Authentication flow (Wallet connect, SIWE).
- **`feature:home`**: Dashboard and overview.
- **`feature:chat`**: AI agent chat interface.
- **`feature:wallet`**: Wallet management and asset display.
- **`feature:trade`**: Token swapping functionality.
- **`feature:yield`**: Staking and yield farming UI.
- **`feature:profile`**: User profile and settings.
- **`feature:settings`**: App-wide settings.
- **`feature:agent`**: Agent-specific configurations.

### Core Modules (`core:*`)
Low-level shared logic.
- **`core:model`**: Shared data classes and entities (used by all modules).
- **`core:domain`**: Business logic, UseCases, and Repository interfaces.
- **`core:data`**: Repository implementations, offline-first logic.
- **`core:network`**: Ktor client configuration and API services.
- **`core:database`**: SQLDelight database definitions.
- **`core:datastore`**: Multiplatform Settings (Key-Value store).
- **`core:designsystem`**: Shared Compose UI components, themes, and icons.
- **`core:ui`**: Shared UI utilities.
- **`core:common`**: General utilities (dispatchers, logging, exceptions).
- **`core:analytics`**: Product analytics tracking.
- **`core:ai`**: Client-side AI processing and parsing.

### Base Platform Modules (`core-base:*`)
Platform-specific implementations of core interfaces using expect/actual.
- **`core-base:ui`**, **`core-base:database`**, **`core-base:network`**, etc.

### Sidecars & Infrastructure
- **`agentkit-sidecar`**: Node.js service for interacting with CDP AgentKit.
