# Skill Routing Map (KMP)

Use this map to select the correct agent for KMP tasks and keep execution phase-aware.

## Keyword to Agent Mapping

| Keywords | Primary Agent | Notes |
|---|---|---|
| plan, phase, milestone, scope, breakdown | pm-agent | Start here for multi-step work |
| architecture, module boundaries, expect/actual, ADR | architect-agent | Validate design before coding |
| Ktor, repository, data source, Firebase, WalletConnect, signing | backend-agent | Includes service-side integrations |
| Compose web, cmp-web, UI states, navigation, js target | frontend-agent | Web target in KMP context |
| Android, iOS, desktop, platform adapters, expect/actual actualization | mobile-agent | Platform-specific implementation |
| bug, crash, regression, flaky, failing gate | debug-agent | Root-cause and recovery |
| review, security, performance, release-readiness | qa-agent | Final gatekeeper |

## Typical Execution Orders

| Pattern | Order |
|---|---|
| New cross-platform feature | pm -> architect -> implementation agent(s) -> qa |
| Shared module enhancement | pm -> backend/mobile (shared) -> qa |
| Platform-only fix | debug or mobile/frontend -> qa |
| Security-sensitive change | pm -> architect -> implementation -> qa |

## Dependency Rules

- qa-agent runs after implementation unless user asks for review-only.
- architect-agent should precede implementation when module boundaries may change.
- debug-agent can interrupt implementation only for blocking failures.
- Do not run parallel tracks that modify the same module area without explicit coordination.

## Escalation

| Situation | Escalate To |
|---|---|
| Phase mismatch or missing acceptance criteria | pm-agent |
| Architecture conflict | architect-agent |
| Gate failure repeatedly unresolved | debug-agent |
| Security policy ambiguity | qa-agent plus architect-agent |
