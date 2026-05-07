---
description: KMP tool governance workflow for MCP and execution tools, with safe defaults and quality-gate-aware restrictions.
---

# Tools Workflow — Leta-Pay KMP

## Goals
- Keep tool usage safe, auditable, and aligned with KMP delivery.
- Prefer read-only exploration before write operations.
- Prevent bypass of quality/security workflows.

## Tool Policy
- Read-only operations: always allowed.
- Write operations: allowed with task context and clear target files.
- Destructive operations: require explicit user confirmation.

## KMP-Aware Guardrails
When changes touch Kotlin or Gradle files, enforce reminder to run:
- ./gradlew spotlessCheck
- ./gradlew detekt

When changes touch auth/security/storage, enforce reminder to validate:
- token storage policy by platform
- web refresh-token prohibition
- Firebase auth/rules consistency

## MCP Configuration Guidance
- Keep minimal necessary enabled tools for current task.
- Use broader tool permissions only when task complexity requires.
- Revert to safer defaults after high-risk operations.

## Output
Always show:
- current tool access profile
- requested change
- risk level
- confirmation needed (yes/no)
