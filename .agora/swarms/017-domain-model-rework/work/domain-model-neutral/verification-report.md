# Verification report

- Added `renovatio-domain-model` with immutable `DomainModel v1` contract and evidence-bearing nodes.
- Added validation for duplicate/unknown references, confidence bounds, and deterministic canonical hashing.
- Added tests for ordering-independent hashes and invalid relation rejection.
- `mvn -q -pl renovatio-domain-model test`: passed.
- `git diff --check`: passed.
- Tested commit: `1ef53de2`.
