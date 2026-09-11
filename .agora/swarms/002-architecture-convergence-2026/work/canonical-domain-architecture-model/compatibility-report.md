# Compatibility and retirement report

- `DomainModel` remains at schema `1`; persisted v1 data remains valid.
- Unsupported schema versions fail at construction with the supported version in the diagnostic.
- `ArchitectureModel` and `DecisionSet` start at schema `1` and are immutable, ordered contracts.
- `ArchitectureResult.graph()` remains temporarily available for API compatibility, but is rebuilt
  from `ArchitectureModel`; it is no longer an independent authority.
- `ArchitectureTransformer` is deprecated for removal. Its current implementation is the compatibility
  entry point and always enriches the result through `CanonicalProjectionService`.
- Retirement condition: consumers move from `graph()` to `architectureModel()`, after which the graph
  accessor and transformer wrapper can be removed without changing preview/apply semantics.

No DomainModel v1→v2 adapter is shipped: emitting a version the current model rejects would corrupt the
compatibility contract. A future adapter must be introduced together with an actual v2 schema and fixtures.
