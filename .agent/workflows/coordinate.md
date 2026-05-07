---
description: Coordinate multi-agent KMP delivery using Phase 1-9 planning, parallel implementation, and mandatory Gradle quality gates.
---

# Coordinate Workflow — Leta-Pay KMP

## Step 0: Align Scope
1. Identify target phase(s) from plan.md.
2. Confirm affected layers (UI, Feature, Data, Core, Platform).
3. Confirm impacted platforms (Android, iOS, Desktop, Web).

## Step 1: Decompose Work
1. Run PM planning output from `.agent/workflows/plan.yaml`.
2. Split into tasks by domain:
   - architect-agent: structure and dependencies
   - backend-agent: API/Firebase/idempotency
   - mobile-agent + frontend-agent: platform/UI implementation
   - qa-agent: gate validation

## Step 2: Execute In Parallel Where Safe
- Execute independent tasks in parallel.
- Do not allow two agents to edit same file set simultaneously.
- Preserve shared model consistency (typed primitives + Resource<T>).

## Step 3: Verify Gates
Run required checks:
- ./gradlew spotlessApply
- ./gradlew spotlessCheck
- ./gradlew detekt
- ./gradlew commonTest test
- ./gradlew dependencyGuard

## Step 4: Phase Gate Decision
Advance phase only if:
- all required checks are green
- no critical security findings
- all targeted platforms validated

## Step 5: Final Coordination Report
Report:
- completed tasks
- platform status
- gate outputs
- blockers and next-step recommendation
