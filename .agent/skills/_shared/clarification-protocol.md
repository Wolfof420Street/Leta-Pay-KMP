# Clarification Protocol (KMP)

Use this protocol whenever requirements are ambiguous. For this repository, assumptions must preserve KMP architecture, phase sequencing, and quality/security gates.

## Uncertainty Levels

| Level | State | Action |
|---|---|---|
| LOW | Mostly clear | Proceed with project defaults and record assumptions |
| MEDIUM | Partially ambiguous | Present 2-3 options and ask for selection |
| HIGH | Direction unclear or risky | Block and ask explicit clarification questions |

## High-Risk Triggers (Must Ask)

- Phase boundary is unclear (request mixes planning, implementation, and release)
- Security policy implications (token storage, key handling, signing flow)
- Architecture conflicts (shared vs platform module ownership)
- Scope is subjective or unbounded
- Request could bypass mandatory gates (Spotless, Detekt, tests, dependencyGuard)

## Project Defaults (Only for LOW Ambiguity)

- Kotlin Multiplatform with shared-first implementation
- Compose Multiplatform for UI targets
- Firebase and WalletConnect decisions remain unchanged unless user asks
- Work is scoped to one plan phase at a time
- Quality gates are mandatory before marking complete

## Required Clarification Checklist

| Area | Question |
|---|---|
| Phase | Which plan phase does this belong to? |
| Module ownership | Should changes go in shared, platform, or both? |
| Platform scope | Android/iOS/Desktop/Web or subset? |
| Security | Any token/signing/secrets behavior change? |
| Definition of done | Which tests/checks are required for acceptance? |

## Escalation Templates

### LOW

Proceeding with defaults:
- KMP shared-first design
- Existing Firebase/WalletConnect model
- Mandatory quality gates

### MEDIUM

Uncertainty detected: {topic}

Option A: {approach}
Option B: {approach}
Option C: {approach}

Please choose A/B/C.

### HIGH

Cannot proceed safely.

Clarifications needed:
1. {question}
2. {question}
3. {question}

Status: blocked pending clarification.

## Subagent Rule

If running in subagent mode and uncertainty is HIGH, return blocked status and concrete questions. Do not fabricate requirements.
