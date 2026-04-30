# Serena Memory Protocol (KMP)

This project is phase-driven. Memory updates must stay consistent with plan.md and current gate status.

## Primary Sources

| File | Purpose | Update Rule |
|---|---|---|
| plan.md | phase roadmap and acceptance criteria | read before planning or execution |
| .agent/.shared/lessons-learned.md | recurring failures and fixes | append after resolved incidents |
| .planning/research/* | design and investigation records | update when decisions are finalized |

## Rules

1. Read plan.md before starting work.
2. Record assumptions and unresolved questions before implementation.
3. Append-only for lessons and decision logs.
4. Do not overwrite prior decisions without explicit superseding note.
5. On long tasks, checkpoint at each major phase boundary.

## Status Format

## Step N: {description}
- Status: Todo | In Progress | Done | Blocked
- Agent: {agent-name}
- Phase: {phase number or label}
- Evidence: {tests/checks/run output summary}
- Notes: {risks, follow-ups, assumptions}
