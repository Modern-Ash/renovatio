# Technical Roadmap: Weeks 5-8

- Owner: Founding Engineer
- Date: 2026-03-20
- Scope: [MOD-16](/MOD/issues/MOD-16)

## 1. Objectives

1. Convert week 1-4 PoC assets into a repeatable customer-delivery lane.
2. Increase engineering confidence with measurable quality, security, and cost controls.
3. Prepare a production-ready handoff model for post-PoC expansion projects.

## 2. Week-by-week plan

## Week 5: Operational hardening and measurement baseline

### Goals

- Make PoC delivery measurable by default.
- Close tooling gaps that block deterministic dry-run validation.

### Workstreams

1. Build a CI job for SAM build/deploy dry runs in a pinned environment.
2. Add benchmark harness for latency and throughput on the quote API slice.
3. Add cost snapshot script using AWS Cost Explorer or stack-level tagging baseline.
4. Define a standard evidence bundle layout (`tests`, `deploy outputs`, `metrics`, `risk notes`).

### Exit criteria

- Dry-run pipeline passes end-to-end in CI.
- Benchmark report template is generated from one sample run.
- Cost and runtime metrics are attached to the weekly report.

## Week 6: Migration accelerator depth for legacy workloads

### Goals

- Expand beyond one PoC slice into reusable modernization patterns.

### Workstreams

1. Add one additional representative legacy scenario (batch/control-break or DB2-heavy flow).
2. Create mapping templates from COBOL constructs to target Java/Python service patterns.
3. Add regression test pack covering both scenarios with golden datasets.
4. Document effort bands and risk triggers by scenario type.

### Exit criteria

- Two validated modernization scenarios available with repeatable test evidence.
- Pattern catalog published with decision guidance for architecture selection.

## Week 7: Security and enterprise readiness

### Goals

- Ensure enterprise security controls are baked into the default PoC path.

### Workstreams

1. Add IAM least-privilege review and policy boundary documentation for deploy flows.
2. Add baseline security checks (dependency scan, IaC lint/check, secrets scan) to CI.
3. Define data-handling policy for sanitized datasets and audit logging requirements.
4. Prepare security response FAQ for procurement and architecture review boards.

### Exit criteria

- CI includes security gates with pass/fail reporting.
- Security controls and data policy are documented and linked in handoff package.

## Week 8: Pilot-to-production handoff framework

### Goals

- Move from PoC proof to production migration plan with clear commercial and technical controls.

### Workstreams

1. Publish phased migration playbook (wave sequencing, rollback strategy, cutover criteria).
2. Define production SLO/SLA baseline and observability dashboard requirements.
3. Create standard "PoC to implementation" backlog template with effort ranges.
4. Package a board-ready executive summary for funding decision support.

### Exit criteria

- Production handoff playbook signed off internally.
- Implementation backlog template ready for customer-specific instantiation.
- Decision package includes scope, risks, timeline, and investment envelope.

## 3. Cross-cutting KPIs (tracked weekly)

- PoC dry-run success rate in CI (target: >= 95%).
- Regression parity pass rate across sample scenarios (target: 100% on baseline suite).
- Median API latency for sample workloads (target to be baselined in week 5).
- Security gate pass rate (target: 100% before handoff).
- Evidence-bundle completeness score (target: 100% for every customer-ready package).

## 4. Dependencies and risks

- Dependency: SAM-enabled CI environment and AWS account access for dry runs.
- Dependency: Availability of representative sanitized legacy datasets.
- Risk: Overfitting on synthetic examples; mitigation is to onboard real customer-like workloads.
- Risk: Tooling variance across local environments; mitigation is containerized build runners.

## 5. Governance cadence

- Weekly engineering checkpoint: artifact quality and KPI review.
- Bi-weekly business sync: roadmap alignment with AWS Marketplace and sales pipeline needs.
- Escalation trigger: any KPI miss for two consecutive weeks requires roadmap adjustment.
