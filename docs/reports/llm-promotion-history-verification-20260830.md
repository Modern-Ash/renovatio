# Governed promotion history verification

Date: 2026-08-30

The Java 17 focused dependency reactor completed with 132 tests passed and zero failures.

`GovernedPromotionVerifierTest` and the production CLI verify the committed promotion against Git
and Agora history:

- Commit A is an ancestor of Commit B; Commit B is an ancestor of Commit C; Commit C is reachable
  from `HEAD`.
- Commit A changed the exact envelope path and its committed bytes match content and envelope hashes.
- Commit B changed the technical index path and its parsed index equals the runtime authority.
- Commit C changed the exact evidence and approvals paths.
- Evidence is `cache-promotion/success`, produced by `project:owner`, bound to Commit B and to an
  artifact whose committed bytes match its registered SHA-256.
- The committed approval includes project owner, Commit A/B, envelope hash and index hash.

Any mismatch fails closed before a committed cache entry can be returned.
