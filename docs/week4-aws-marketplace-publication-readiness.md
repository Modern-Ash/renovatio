# Week 4 Delivery: AWS Marketplace Publication Readiness

- Owner: Founding Engineer
- Date: 2026-03-20
- Scope: [MOD-16](/MOD/issues/MOD-16)

## 1. Outcome summary

Week 4 finalized the technical publication package for the ModernAsh AWS Marketplace motion and closed the remaining engineering readiness checks from week 3 handoff.

## 2. Final listing package

### 2.1 Source artifacts

- Core listing copy package:
  - `content/aws-marketplace/2026-03-20-marketplace-listing-copy.md`
- Legacy modernization long-form listing:
  - `root/modernash-aws-marketplace-product.md`
- DevEngage.AI long-form listing:
  - `root/modernash-devengage-ai-aws-marketplace-product.md`
- Week 3 engineering baseline:
  - `renovatio/docs/week3-marketplace-materials-and-deployment-automation.md`
  - `renovatio/docs/week3-technical-differentiators.md`

### 2.2 Final engineering position for listing

- Offer type: AWS Marketplace Professional Services.
- Technical promise: fixed-scope modernization PoC with deterministic parity validation and deployment automation assets.
- Differentiation anchor: move from "code conversion output" to "production-decision evidence" in 10 business days.

## 3. Technical validation evidence

### 3.1 PoC test evidence

Validation executed on 2026-03-20 in `renovatio/`:

- Command: `python3 -m unittest discover -s examples/legacy-modernization-poc/tests -p "test_*.py"`
- Result: `Ran 3 tests ... OK`

### 3.2 Deployment automation checks

- Script syntax validated:
  - `bash -n scripts/deploy-marketplace-poc.sh` -> pass
- Environment tooling check:
  - `aws` CLI available
  - `sam` CLI not installed in current workspace runtime

Implication:

- Build/deploy automation is implemented and lint-clean, but a full SAM build/deploy dry run still requires a workstation or CI runner with `sam` installed.

## 4. Publish-readiness checklist

| Item | Status | Notes |
|---|---|---|
| Listing copy complete for both services | Done | Consolidated in `content/aws-marketplace/2026-03-20-marketplace-listing-copy.md` |
| Technical differentiator narrative ready | Done | Week 3 differentiator matrix available |
| Sample PoC workload and tests available | Done | Deterministic tests passing |
| Deployment automation assets available | Done | Script + CI workflow delivered |
| Dry run deployment evidence captured | Partial | Pending SAM-enabled environment |
| Cost envelope documented | Done | See cost envelope in section 5 |
| Security/procurement objection handling | Done | Covered in week 3 docs and listing copy |

## 5. Cost envelope for sample PoC stack

Assumptions: low-to-moderate demo traffic, `us-east-1`, one Lambda function, one HTTP API route, one DynamoDB table (on-demand).

| Environment | Estimated monthly range (USD) | Main cost drivers |
|---|---:|---|
| Dev | 5-20 | API calls, Lambda requests/duration, minimal DynamoDB storage |
| Stage | 15-60 | Repeat validation traffic, additional logs and test runs |
| Prod pilot | 40-180 | Higher request volume, sustained DynamoDB R/W, observability retention |

Rule of thumb: the PoC stack is intentionally low-cost; real spend variance is dominated by request volume and log retention policy.

## 6. Week 4 completion decision

Engineering package is ready for AWS Marketplace publication workflow with one operational caveat:

- Run one final SAM-based dry run in a CI or workstation environment with `sam` installed and attach resulting stack outputs to the sales handoff.

No additional code changes are required before listing submission.
