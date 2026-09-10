# Week 4 Delivery: PoC Lessons Learned

- Owner: Founding Engineer
- Date: 2026-03-20
- Scope: [MOD-16](/MOD/issues/MOD-16)

## 1. Context

Lessons below reflect execution across week 1 through week 4 for the Legacy Modernization AWS PoC motion, including pipeline setup, sample workload delivery, marketplace packaging, and publication-readiness checks.

## 2. What worked

### 2.1 Fixed-scope slice accelerated decision quality

- Constraining the PoC to one representative workflow reduced ambiguity and made acceptance criteria enforceable.
- Buyers get a funding decision signal faster when outputs are explicit: parity evidence, deployable artifact, and next-phase backlog.

### 2.2 Golden-dataset parity testing created credibility

- Deterministic business-rule validation was more persuasive than code-generation demos.
- Keeping tests in the PoC package made quality visible to both engineering and non-engineering stakeholders.

### 2.3 Delivery automation improved repeatability

- Local deployment script plus CI workflow produced a clear handoff path from demo to customer environment.
- OIDC-based AWS auth avoided long-lived credentials in automation flows.

### 2.4 Technical differentiation stayed grounded in execution

- Positioning around risk controls (fidelity, rollback, cutover gates) landed better than generic AI productivity claims.
- Marketplace narrative benefited from concrete engineering artifacts, not marketing-only statements.

## 3. Friction points and gaps

### 3.1 Toolchain completeness varies by runtime

- `sam` CLI is not guaranteed in all execution environments, which can delay full dry-run evidence collection.
- Mitigation: run SAM build/deploy validations in a pinned CI runner image and publish outputs automatically.

### 3.2 Cost/performance telemetry is still manual

- Cost envelope exists, but no automated per-environment cost snapshot is generated yet.
- Latency and throughput benchmarks are not yet part of the default PoC report.

### 3.3 Evidence packaging needs one canonical bundle

- Artifacts exist across multiple directories (`renovatio/docs`, `content/aws-marketplace`, `root/`).
- Sales and delivery teams need one indexable handoff package to reduce operational friction.

## 4. Decisions adopted for next phase

1. Keep fixed-scope, evidence-first PoC model as standard go-to-market package.
2. Treat parity tests and deployment automation as mandatory deliverables, not optional extras.
3. Introduce CI-owned dry-run evidence generation for Marketplace readiness.
4. Add baseline observability and cost telemetry into the PoC default acceptance criteria.
5. Standardize a single handoff artifact index for sales, delivery, and procurement stakeholders.

## 5. Reusable operating principles

- Prove behavior, not just transformed code.
- Keep scope narrow, validation depth high.
- Automate the path that the customer must trust.
- Tie every technical output to a commercial decision point.
