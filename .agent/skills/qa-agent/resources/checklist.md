# QA Review Checklist

Complete every item before final sign-off.

## Security and Policy
- [ ] Auth, authorization, and token handling follow policy.
- [ ] Inputs are validated and injection risks are mitigated.
- [ ] Secrets and sensitive data are not exposed in code, logs, or errors.
- [ ] Dependency and configuration risks are reviewed.

## Performance and Reliability
- [ ] Critical flows meet expected responsiveness and stability targets.
- [ ] Data access and rendering paths show no obvious bottlenecks.
- [ ] Error recovery, retry, and offline behavior are validated where applicable.

## Accessibility and UX
- [ ] Keyboard navigation and focus order are valid.
- [ ] Labels, semantics, and assistive technology behavior are verified.
- [ ] Contrast and responsive behavior are acceptable for target viewports.
- [ ] Loading, error, and empty states are clear and usable.

## Test and Quality Coverage
- [ ] Unit and integration coverage supports changed behavior.
- [ ] Critical end-to-end flows are validated.
- [ ] No flaky or non-reproducible findings are reported.
- [ ] `../_shared/verify.sh` passes.

## Findings Quality
- [ ] Findings are ordered by severity: CRITICAL, HIGH, MEDIUM, LOW.
- [ ] Each finding includes file location, impact, and remediation guidance.
- [ ] Final report clearly states PASS, WARNING, or FAIL.
