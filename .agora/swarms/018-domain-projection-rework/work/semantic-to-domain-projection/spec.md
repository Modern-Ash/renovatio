# SemanticProgram to DomainModel specification

## Outcome

Project the existing target-neutral `SemanticProgram` into `DomainModel v1` using deterministic evidence-based rules. The projector creates a useful business abstraction without changing any existing target emitter or generation path.

## Deterministic mappings

- Program header → aggregate and primary entity.
- Data intents/types → value objects or domain invariants when their semantic kind supports it.
- IO operations → repository/external-system nodes according to IO kind.
- Side effects and control-flow presence → use-case/domain-service nodes and relations.
- Every node references the source provenance path and source span.

Ambiguous classifications remain explicit action items for the next layer; they are not silently promoted to business facts.

## LLM boundary

This work item exposes only a projection seam and structured suggestion payload boundary. A future LLM adapter may propose names or classifications, but responses must be schema-validated, attributed, cached, and persisted as `NEEDS_REVIEW` decisions. No final source code is generated here.

## Compatibility

The projector is additive and pure. Existing Java/Node generation continues to consume `SemanticProgram`/`TargetModel` unchanged.
