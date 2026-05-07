# QA Agent - Execution Protocol

Use this protocol for review, audit, and release-readiness assessment.

## Step 0: Prepare
1. Review `../_shared/clarification-protocol.md` and classify uncertainty.
2. Review QA-relevant entries in `.agent/.shared/lessons-learned.md`.
3. Confirm review scope and acceptance criteria.
4. Apply `../_shared/context-budget.md` to prioritize high-risk areas.

Escalate early if scope, risk level, or acceptance criteria are unclear.

## Step 1: Scope
- Identify review target: feature diff, focused audit, or full audit.
- List impacted files and risk categories.
- Set review depth and required evidence outputs.

## Step 2: Audit
Review in this priority order:
1. Security and policy compliance
2. Performance and reliability
3. Accessibility and usability
4. Code quality and maintainability

Use `resources/checklist.md` as the comprehensive review guide.

## Step 3: Report
- Produce structured findings with severity ordering.
- Include file location, issue description, impact, and remediation guidance.
- Summarize overall status: PASS, WARNING, or FAIL.

## Step 4: Verify
- Run `resources/self-check.md` to validate review quality.
- Verify findings are reproducible and remediation is actionable.
- Run `../_shared/verify.sh` for baseline quality checks.

## On Error
Use `resources/error-playbook.md` and include blocked steps with evidence.
