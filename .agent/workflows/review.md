---
description: KMP review workflow: prioritize findings by severity, verify architecture integrity, run quality gates, and report release risks.
---

# Review Workflow — Leta-Pay KMP

## Step 1: Scope
Review a feature, branch, or full repository.

## Step 2: Correctness and Regression Risk
- Verify changed behavior against acceptance criteria.
- Identify likely regressions in shared logic and platform source sets.

## Step 3: Architecture Review
- Check layer boundaries.
- Check expect/actual placement.
- Check forbidden cross-feature imports.

## Step 4: Quality Gate Review
- spotless status
- detekt status
- test status
- dependencyGuard status

## Step 5: Security Review
- token/session handling compliance
- platform storage policy compliance
- Firebase rule and auth flow consistency
- idempotency on mutation endpoints

## Step 6: Report Findings First
Report in severity order:
- CRITICAL
- HIGH
- MEDIUM
- LOW

Each finding includes:
- file reference
- risk summary
- concrete remediation

## Step 7: Residual Risks
If no findings, still report:
- testing gaps
- unverified assumptions
- recommended follow-up checks
