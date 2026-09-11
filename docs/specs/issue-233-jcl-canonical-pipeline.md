# Issue 233 JCL Canonical Pipeline Contract

Issue #233 is the AC-12 convergence pass for the existing F7 `renovatio-jcl`
module. F7 already introduced deterministic parsing, batch projection, utility
handling and Spring Batch emission. AC-12 tightens the integration contract:
JCL remains an experimental deterministic batch capability, but it must no
longer depend directly on the LLM runtime or require a separate transport path.

## Binding Scope

- `renovatio-jcl` is a deterministic batch/domain library. Its production
  dependencies may include the semantic IR, profile and decisions domains, but
  not `renovatio-llm`.
- Batch ambiguity handling is expressed through the decisions-domain
  `DecisionSuggestionPort`. The LLM runtime may implement that port from
  outside JCL, but JCL does not import runtime classes.
- Application preview is the canonical place where target artifacts and batch
  orchestration artifacts are combined into one manifest. CLI, API and MCP can
  continue using their existing application-service boundary.
- With no `batch.target` or no JCL-derived projection, behavior remains inert
  and defaults-safe.
- Spring Batch remains experimental unless the generated manifest and
  characterization evidence are present for a concrete project.

## Acceptance Mapping

| Issue criterion | Binding for this pass |
|---|---|
| `deterministic-core` | Maven dependency and source tests prove `renovatio-jcl` does not depend on or import `renovatio-llm`. |
| `semantic-projection` | Existing `BatchJobProjection` remains the deterministic Semantic IR bridge; this pass documents and preserves it. |
| `proposal-adapter` | `BatchSuggestionAdapter` accepts the common decisions-domain port and filters only `BATCH` decisions. |
| `application-flow` | `DefaultRenovatioApplication.preview` invokes a batch orchestration port before refinement and manifest hashing. |
| `spring-batch` | Existing `SpringBatchBatchEmitter` remains behind the batch planner/emitter boundary; unsupported constructs remain action items/residue. |
| `fixtures` | Existing F7 fixtures remain authoritative; no syntax expansion is introduced without tests. |
| `characterization` | The report records the focused module suite and the existing characterization surface. |

## Non Goals

- No new JCL grammar expansion.
- No direct LLM invocation from JCL parsing, projection or emission.
- No separate CLI/API endpoint just for JCL orchestration.
- No promotion of Spring Batch from experimental to supported.
