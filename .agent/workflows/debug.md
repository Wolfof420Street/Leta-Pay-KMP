---
description: KMP debugging workflow: isolate shared vs platform issue, find root cause, apply minimal fix, and validate with regression tests and Gradle gates.
---

# Debug Workflow — Leta-Pay KMP

## Step 1: Classify the Failure
- Is this in commonMain or platform-specific source set?
- Which platforms reproduce the issue?
- What is expected vs actual behavior?

## Step 2: Reproduce
- Reproduce consistently.
- Capture stack traces, logs, and environment details.

## Step 3: Root Cause Investigation
- Build 3-5 hypotheses ranked by probability.
- Eliminate hypotheses with evidence.
- Confirm root cause before editing code.

## Step 4: Minimal Fix
- Apply smallest safe fix that addresses root cause.
- Preserve layer boundaries and typed domain model usage.

## Step 5: Regression Coverage
- Add or update regression tests.
- Validate across relevant source sets.

## Step 6: Quality Gate Validation
- ./gradlew spotlessApply
- ./gradlew spotlessCheck
- ./gradlew detekt
- ./gradlew commonTest test

## Step 7: Similar Pattern Scan
- Search related modules for same anti-pattern.
- Patch confirmed duplicates with tests.

## Step 8: Report
Include:
- root cause
- fix summary
- tests added
- gate outcomes
- residual risk
