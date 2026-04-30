---
description: KMP planning workflow: map request to Phase 1-9, define architecture-safe tasks, and output prioritized execution plan with quality gates.
---

# Plan Workflow — Leta-Pay KMP

## Step 1: Requirements Intake
Capture:
- business objective
- target phase alignment (1-9)
- platform impact (Android/iOS/Desktop/Web)
- backend impact (Vercel/Firebase)
- risk level

## Step 2: Codebase Mapping
Use map-codebase workflow output to identify:
- modules to touch
- reusable existing patterns
- architecture constraints

## Step 3: Task Decomposition
Create tasks with:
- owner agent
- affected modules/files
- acceptance criteria
- dependencies
- platform coverage expectations

## Step 4: API and Data Contracts
Define or validate:
- build vs broadcast endpoints
- Firebase schema/rules impact
- token/session handling implications

## Step 5: Quality and Security Criteria
Attach required checks:
- spotless, detekt, tests, dependencyGuard
- security checks for storage/token handling

## Step 6: Approval Package
Present plan with:
- P0/P1/P2 priorities
- execution waves
- phase gate checklist
- rollback/debug path

## Step 7: Save Plan
Persist machine-readable plan for orchestrate workflow consumption.
