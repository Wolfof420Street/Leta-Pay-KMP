---
description: Execute approved KMP plans in parallel waves with strict sequencing, platform consistency checks, and mandatory quality gates.
---

# Orchestrate Workflow — Leta-Pay KMP

## Step 0: Load Approved Plan
- Require approved plan from `/plan` output.
- Validate dependency order and phase gate prerequisites.

## Step 1: Build Execution Waves
- Group independent tasks into waves by dependency.
- Assign agents by capability:
  - architecture concerns -> architect-agent
  - backend/Firebase concerns -> backend-agent
  - platform implementation -> mobile-agent/frontend-agent
  - verification -> qa-agent

## Step 2: Execute
- Run wave N in parallel.
- Stop next wave if any task in current wave fails hard.

## Step 3: Verify Each Wave
Run:
- ./gradlew spotlessCheck
- ./gradlew detekt
- targeted tests for changed modules

## Step 4: Final Full Validation
Run:
- ./gradlew commonTest test dependencyGuard

## Step 5: Consolidated Report
Include:
- wave completion matrix
- files changed per wave
- quality status
- blocked items and recommended rerun scope
