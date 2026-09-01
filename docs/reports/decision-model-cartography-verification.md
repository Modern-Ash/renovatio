# F0 Decision-Model Cartography — Verification Report

- **Work item:** `decision-engine/f0-decision-cartography`
- **Specification:** `docs/specs/decision-model-cartography.md`
- **Implementation plan:** `docs/plans/decision-model-cartography.md`
- **Verification date:** 2026-08-31
- **Scope:** Documentation and evidence only
- **Overall result:** PASS

## Baseline reproduction

| Check | Expected | Observed | Result |
|---|---|---|:---:|
| Repository revision | `f2173f5ef1ffde7e6c1a35439ab66e49c27877e6` | Exact match from `git rev-parse HEAD` | PASS |
| `JavaGenerationService.java` SHA-256 | `8b3fb65abb085485b4a9fb5a6461c457110899b8f550db00cb1da8c7c0a2ad2d` | Exact match | PASS |
| `MigrationPlanService.java` SHA-256 | `fa12b7477ad05d6b74041c6e62bd58c6bc2f210500af2c39aa54e3d5e877bd93` | Exact match | PASS |
| Fixture inputs | 13 | 13 `input.cob` files | PASS |
| Behavior evidence | One per fixture | 13 `expected-behavior.json` files | PASS |
| Action-item evidence | One per fixture | 13 `expected-action-items.json` files | PASS |
| IR goldens | 10, with three declared gaps | 10 `expected-ir.json` files | PASS |
| Annotated/generated-Java goldens | Two fixture families | `data-intent-redefines` and `move-numeric` | PASS |

Both service files were already modified in the working tree when the
cartography snapshot was taken. Their working-tree content is deliberately
pinned by the hashes above. This spike did not modify either production file.

## Acceptance-criterion results

### `catalog` — PASS

An `awk` table check found 38 numbered catalog rows and zero rows with an
unexpected number of columns. This exceeds the required 15 decisions. The
columns cover category, typical location, current option, alternatives,
confidence, LLM-delegation recommendation, characterization flag, and
frequency.

### `categories` — PASS

The real-fixture example table contains exactly one explicit evidence row for
each required category: NUMERIC, CONTROL_FLOW, DATA_SHAPE, PERSISTENCE,
NAMING, and ARCHITECTURE. The PERSISTENCE row correctly cites working-storage
lifetime and preserves the gap for file, VSAM, SQL, and CICS examples.

### `coupling-map` — PASS

The service hashes reproduced exactly and both source files were re-read. The
map covers:

- Direct arguments and stored caller inputs.
- Constructor collaborators and their transitive parser/IR/annotation/runtime
  rules.
- Query and workspace metadata, parser result metadata, and generated-result
  metadata.
- Filesystem content, output/report writes, existence checks, locale, standard
  streams, UUIDs, clocks, and in-memory plan/run maps.
- Hard-coded generation choices, fixed migration steps, ignored/unused inputs,
  and the observed dry-run behavior.

No additional consumed-input class was found outside the map at the captured
content hashes.

### `f1-recommendation` — PASS

The strict rule was recomputed directly from the catalog columns:

1. Frequency is at least 3, or applicability is structural to all programs.
2. Confidence is H.
3. The decision is characterization-verifiable (`yes`).

The independent `awk` selection returned, in order:

```text
1
27
28
30
33
37
38
```

This exactly matches the seven-item recommendation in the specification. No
manual exception or desirable-cluster override was applied.

## Commands used

```bash
git rev-parse HEAD
sha256sum renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/service/JavaGenerationService.java renovatio-provider-cobol/src/main/java/org/shark/renovatio/provider/cobol/service/MigrationPlanService.java
rg --files renovatio-provider-cobol/src/test/resources/characterization -g 'input.cob'
rg --files renovatio-provider-cobol/src/test/resources/characterization -g 'expected-ir.json'
rg --files renovatio-provider-cobol/src/test/resources/characterization -g 'expected-behavior.json'
rg --files renovatio-provider-cobol/src/test/resources/characterization -g 'expected-action-items.json'
awk -F'|' '/^\| [0-9]+ \|/{rows++; if (NF != 12) bad++} END {print rows, bad+0}' docs/specs/decision-model-cartography.md
git diff --check -- docs/specs/decision-model-cartography.md docs/plans/decision-model-cartography.md docs/reports/decision-model-cartography-verification.md
```

The F1 selection was also performed with `awk` over the catalog fields for
confidence, characterization, and frequency/structural applicability.

## Conclusion

All four acceptance criteria are implemented by the governed specification and
supported by reproducible evidence. The work is ready for Agora verification.
