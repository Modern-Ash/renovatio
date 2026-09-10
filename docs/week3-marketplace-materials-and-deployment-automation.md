# Week 3 Delivery: AWS Marketplace Materials and Deployment Automation

- Owner: Founding Engineer
- Date: 2026-03-20
- Scope: MOD-13 week 3 deliverables

## 1. Week 3 outcomes

This package delivers:

1. A production-ready AWS Marketplace collateral set for the Legacy Modernization PoC.
2. A deployable sample PoC workload aligned to the Marketplace offer scope.
3. End-to-end deployment automation (local script + CI workflow).
4. Competitor-facing technical differentiators to support sales conversations.

## 2. Marketplace listing materials

### 2.1 Product baseline

- Offer type: Professional Services.
- Product name: ModernAsh Legacy Modernization PoC - AI-Assisted Fixed-Scope Engagement on AWS.
- Category fit: Legacy modernization and application transformation.
- Buyer objective: de-risk modernization funding decisions with measurable evidence in 10 business days.

### 2.2 Listing copy blocks

#### Short description (<= 200 chars option)

ModernAsh delivers a fixed-scope legacy modernization PoC on AWS with parity validation, deployment automation, and a migration roadmap in 10 business days.

#### Full description positioning

- We modernize one representative legacy workflow and prove business-rule fidelity.
- We package operational controls: observability, rollback gates, and acceptance criteria.
- We provide implementation-ready backlog and migration economics snapshot for the next funding step.

### 2.3 Included deliverables in offer page

- Discovery and dependency mapping for selected workload.
- One end-to-end modernization slice with golden-dataset validation.
- Deployment automation assets for repeatable customer demos.
- Executive readout with modernization roadmap and phased expansion options.

### 2.4 Evidence checklist for publish readiness

- [x] Public-facing product narrative and scope in `root/modernash-aws-marketplace-product.md`.
- [x] Value messaging and risk framing from week 2 positioning work.
- [x] Deployable sample workload with test evidence.
- [x] Deployment runbook and CI deployment path for repeatability.
- [x] Technical differentiators document for field teams.

## 3. Deployment automation package

### 3.1 New assets

- Local deployment script: `renovatio/scripts/deploy-marketplace-poc.sh`
- CI deployment workflow: `renovatio/.github/workflows/marketplace-poc-sam-deploy.yml`
- Sample infrastructure template: `renovatio/examples/legacy-modernization-poc/template.yaml`

### 3.2 Automation behavior

- Validates local prerequisites (`python3`, `sam`, `aws`).
- Runs unit tests against golden dataset before deploy.
- Builds SAM application and deploys with environment parameterization.
- Uses OIDC role assumption in GitHub Actions for AWS authentication.

### 3.3 Standard deployment command

From `renovatio/`:

```bash
ENVIRONMENT=dev AWS_REGION=us-east-1 ./scripts/deploy-marketplace-poc.sh
```

## 4. Sample PoC code package

### 4.1 Use case

Legacy insurance premium calculation modernized into an API-backed workload:

- Input: policy pricing data.
- Processing: deterministic rating logic with parity validation path.
- Output: premium quote + decision (`approve` / `review`).
- Persistence: quote audit entries in DynamoDB.

### 4.2 Sample assets

- `renovatio/examples/legacy-modernization-poc/src/app.py`
- `renovatio/examples/legacy-modernization-poc/tests/golden_dataset.json`
- `renovatio/examples/legacy-modernization-poc/tests/test_app.py`
- `renovatio/examples/legacy-modernization-poc/README.md`

### 4.3 Validation commands

```bash
python3 -m unittest discover -s examples/legacy-modernization-poc/tests -p "test_*.py"
bash -n scripts/deploy-marketplace-poc.sh
```

## 5. Hand-off notes for week 4

1. Execute a full dry run in target AWS account and capture CloudFormation outputs for listing evidence.
2. Add cost estimate table (dev/stage/prod) from deployed sample stack.
3. Finalize FAQ content and objection handling for procurement and security review.
