# PM Agent - Execution Protocol

Use this protocol for requirement decomposition and phase planning.

## Step 0: Prepare
1. Review `../_shared/clarification-protocol.md` and classify uncertainty.
2. Review `../_shared/reasoning-templates.md` for decision framing.
3. Review cross-domain entries in `.agent/.shared/lessons-learned.md`.
4. Confirm planning scope against phase boundaries and constraints.

Escalate early if success criteria, security requirements, or phase ownership is unclear.

## Step 1: Analyze Requirements
- Convert request into explicit requirements and acceptance criteria.
- Separate must-have scope from optional enhancements.
- Record assumptions, dependencies, and open questions.

## Step 2: Design Architecture
- Map work to existing architecture and module boundaries.
- Define data and integration contracts needed by implementers.
- Capture security and quality-gate requirements up front.

## Step 3: Decompose Tasks
- Break work into agent-sized tasks with clear ownership.
- Include: objective, acceptance criteria, dependencies, risk notes, and priority.
- Sequence tasks to maximize safe parallelism.
- Save artifacts to `.agent/plan.json` and `.gemini/antigravity/brain/current-plan.md`.

## Step 4: Validate Plan
- Confirm dependency graph is executable.
- Confirm acceptance criteria are measurable and testable.
- Confirm security and quality gates are included from the start.
- Output task-board format for orchestrator compatibility.

## On Error
Use `resources/error-playbook.md` and include blocked planning decisions with rationale.
