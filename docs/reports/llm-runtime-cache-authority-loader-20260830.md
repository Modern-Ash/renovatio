# Runtime committed-cache authority loader verification

Date: 2026-08-30

## Result

The Java 17 focused dependency reactor completed with 131 tests passed and zero failures.

## Verified behavior

- The production CLI loads `committed-cache-index.v1.json` and
  `verified-cache-promotion.v1.json` exclusively from Git `HEAD`.
- Stored index entries must exactly equal an index regenerated from committed envelopes.
- Manifest index digest and key set must exactly match the committed index.
- Missing paired authority files, malformed files and tree drift fail closed.
- A repository with no committed cache authority receives an empty lookup view and follows the
  governed miss path.
- CLI tests execute against a real temporary Git repository with a committed `HEAD`.

The remaining promotion Commit A/B/C workflow is intentionally not simulated by this report.
