# Verification report

- Added `SemanticDomainProjector`, a pure conservative projection from `SemanticProgram` to `DomainModel v1`.
- Programs become aggregate/use-case nodes; file/database I/O becomes repository boundaries; all nodes retain source provenance.
- LLM remains an explicit future adapter boundary and is not invoked by deterministic projection.
- `mvn -q -pl renovatio-domain-model -am test`: passed.
- `git diff --check`: passed.
- Tested commit: `082aaeb3`.
