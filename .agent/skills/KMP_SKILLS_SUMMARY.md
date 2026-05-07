# KMP Skills Overhaul - Summary

## What Was Created

You now have **7 KMP-specific skills** replacing generic non-KMP mobile/web skills, tailored to your **Kotlin Multiplatform architecture** and **9-phase plan**.

### New Skills

| Skill | Focus | When to Use |
|-------|-------|-----------|
| **shared-kmp-core** | Domain logic, models, errors, typed values, repositories | Building reusable business logic for all platforms |
| **kmp-platform-specifics** | Expect/actual implementations, Keychain, FCM, APNs | Platform-specific native code (Android/iOS/Desktop/Web) |
| **kmp-ui-compose** | Compose Multiplatform screens, components, navigation, Material 3 | Building UIs that work on all 4 platforms |
| **firebase-and-realtime** | Firebase Realtime DB, Ktor HTTP client, custom auth, push tokens | Backend integration and real-time sync |
| **kmp-module-architecture** | Module structure, dependency management, gradle setup | Organizing code, preventing circular deps |
| **phase-orchestrator** | Phase 1-9 execution, deliverables, gate criteria | Planning and tracking phase-based work |
| **kmp-testing** | Unit/integration/E2E tests, mocks, test infrastructure | Testing across all platforms |

### Bonus: Routing Guide

**File:** `KMP_SKILLS_ROUTING.md`

A visual decision tree showing which skill to use for any task + real-world scenarios combining multiple skills.

---

## How They Connect to Your Architecture

Each skill directly references your **plan.md** structure:

### Shared-KMP-Core
- ✅ Your typed value classes (ChainId, WalletAddress, TxHash, AssetId)
- ✅ Your AppError hierarchy
- ✅ Your Resource<T> wrapper
- ✅ Your TransactionStatus state machine
- ✅ Your module structure (shared/core, shared/feature, shared/ui)

### KMP Platform Specifics
- ✅ Android (EncryptedSharedPreferences, FCM)
- ✅ iOS (Keychain, APNs, CocoaPods)
- ✅ Desktop (encrypted local files)
- ✅ Web (sessionStorage, Web Push API)
- ✅ Your security model (sessionToken persisted, NO refreshToken on web)

### KMP UI Compose
- ✅ Your 5 main screens (Chat, Wallet, Trade, Yield, Account)
- ✅ Your Material 3 design system
- ✅ Loading skeletons, error states
- ✅ Resource<T> pattern for async UI states

### Firebase & Realtime
- ✅ WalletConnect → Firebase custom auth flow
- ✅ Build vs Broadcast separation for transactions/swaps
- ✅ Firebase Realtime DB for chat with local SQLDelight sync
- ✅ Offline queue with retry logic
- ✅ Device token registration endpoints

### KMP Module Architecture
- ✅ Your exact folder structure (cmp-shared/src/commonMain, androidMain, iosMain, desktopMain, jsMain)
- ✅ Layer organization (UI → Feature → Data → Core → Platform)
- ✅ No circular dependencies
- ✅ Placement rules (where does X code go?)

### Phase Orchestrator
- ✅ All 9 phases from plan.md (Setup, WalletConnect, Chat, Send Payment, Swaps, Staking, Push, Polish, Deploy)
- ✅ Deliverables for each phase
- ✅ Gate criteria
- ✅ Phase dependencies

### KMP Testing
- ✅ Your error types (AppError.InsufficientBalance, QuoteExpired, etc.)
- ✅ Your models (ChainId, WalletAddress, ChatMessage, etc.)
- ✅ Your async pattern (Resource<T>, StateFlow, Flow)
- ✅ Mock infrastructure (MockFirebaseDatabase, MockHttpClient, MockSecureStorage)

---

## Skills Include

Each skill SKILL.md file contains:

✅ **When to Use** – Clear trigger phrases  
✅ **When NOT to Use** – What other skills handle  
✅ **Core Rules** – Best practices for KMP  
✅ **Code Examples** – Copy-paste templates  
✅ **Architecture Patterns** – Your specific patterns (expect/actual, Resource<T>, etc.)  
✅ **Platform Differences** – Android vs iOS vs Desktop vs Web  
✅ **Common Pitfalls** – What goes wrong (with fixes)  
✅ **Verification Checklist** – How to know you're done  
✅ **References** – Links to execution protocols & resources  

---

## Using the Skills

### Option 1: Direct Skill Request
```
/shared-kmp-core
I need to create a typed value class for transaction amounts.
```

### Option 2: Task-Based Discovery
Copilot will automatically detect applicable skills based on your request:
```
Implement wallet balance display across all platforms
→ Skills detected: shared-kmp-core, firebase-and-realtime, kmp-ui-compose, kmp-testing
```

### Option 3: Routing Guide Reference
```
Read KMP_SKILLS_ROUTING.md to see real-world scenarios showing which skills to combine
```

---

## What Changed from Generic Skills

| Generic | KMP-Specific |
|---------|-------------|
| "Generic mobile/web implementation" | "Kotlin Multiplatform core logic" |
| "Generic repository pattern" | "Resource<T>, AppError, typed value classes" |
| "Standard UI patterns" | "Compose Multiplatform, Material 3 theme per your design" |
| "Generic testing" | "KMP expect/actual testing, platform-specific mocks" |
| "Generic module layout" | "Your exact architecture: shared/core/feature/ui with no circular deps" |
| "Generic phases" | "Your 9-phase plan with concrete deliverables & gates" |

---

## Getting Started with Skills

1. **Review the routing guide**: `KMP_SKILLS_ROUTING.md`
   - See decision tree: Which skill for my task?
   - See real-world scenarios: Multi-skill workflows

2. **Start with Phase 1**: Use `/phase-orchestrator`
   - Understand deliverables (DI, HTTP client, Root auth)
   - Know gate criteria (all 4 platforms build, Koin resolves)

3. **Implement layered**: Use skills in order (Core → Platform → UI → Tests)
   - Create models with `/shared-kmp-core`
   - Implement platforms with `/kmp-platform-specifics`
   - Build UI with `/kmp-ui-compose`

4. **Move through phases**: Each phase unlocks next phase
   - Phase 1 → All 4 platforms build
   - Phase 2 → WalletConnect works
   - Phase 3 → Chat syncs with Firebase
   - ...continuing through Phase 9

---

## Files Location

All skills are in:
```
.agent/skills/
├── shared-kmp-core/SKILL.md
├── kmp-platform-specifics/SKILL.md
├── kmp-ui-compose/SKILL.md
├── firebase-and-realtime/SKILL.md
├── kmp-module-architecture/SKILL.md
├── phase-orchestrator/SKILL.md
├── kmp-testing/SKILL.md
└── KMP_SKILLS_ROUTING.md  ← Start here
```

---

## Next Steps

### Immediately
1. ✅ **Read routing guide**: `KMP_SKILLS_ROUTING.md` (5 min)
2. ✅ **Pick a starting skill**: Probably `/phase-orchestrator` or `/shared-kmp-core`
3. ✅ **Try a request**: "What are Phase 1 deliverables?" with `/phase-orchestrator`

### In Your Development
- Use skill routing guide to select correct skill for each task
- Skills reference plan.md, so they're always aligned with your architecture
- Combine multiple skills for complex features (Full feature = 2-3 skills in sequence)
- Let skills guide code organization, patterns, and validation

### Over Time
- Skills evolve with your codebase via lessons-learned
- Update routing guide as new patterns emerge
- Archive completed phases in skill resources

---

## Quality Checkpoints

Each skill includes **checklists** to verify you're done:

- ✅ Code compiles on all 4 platforms
- ✅ No circular dependencies
- ✅ No platform-specific code in commonMain
- ✅ All expect/actual implemented
- ✅ Error handling exhaustive (all paths map to AppError)
- ✅ Tests cover >70% critical paths
- ✅ Phase gate criteria pass

---

## Skill Philosophy

These skills embody **your project's constraints and decisions**:

- ✅ **KMP-native** – Expect/actual, commonMain/platformMain
- ✅ **Typed** – ChainId, WalletAddress (no stringly typed)
- ✅ **Layered** – UI → Feature → Data → Core → Platform (acyclic)
- ✅ **Phase-driven** – Structured 9-phase rollout
- ✅ **Multi-platform** – Android, iOS, Desktop, Web from day 1
- ✅ **Offline-first** – SQLDelight + Firebase sync + pending queue
- ✅ **Non-custodial** – WalletConnect + no key storage
- ✅ **Tested** – Unit/integration/E2E across all platforms

---

## Support

If a skill doesn't cover your exact use case:
- Check **KMP_SKILLS_ROUTING.md** for similar scenarios
- Look for **"Common Pitfalls"** section (often has your answer)
- Link to your specific **plan.md** section in your request
- Skills can be refined based on feedback from actual usage

---

**Summary**: You now have 7 specialized KMP skills + routing guide = full development workflow from Phase 1 through Phase 9, from code to tests to deployment. Happy building! 🚀

---

**Created:** April 2026  
**Project:** Leta-Pay-KMP (Kotlin Multiplatform)  
**Skills:** 7 KMP-specific + 1 routing guide  
**Based on:** plan.md (MVP + 9-phase roadmap)  
**Platforms:** Android, iOS, Desktop (JVM), Web (JS)
