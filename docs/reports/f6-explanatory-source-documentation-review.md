# Review: F6 explanatory source documentation

## Verdict

No blocking findings. The increment is ready for Spec Owner acceptance.

## Confirmed checks

- The extension is opt-in and strictly boolean; legacy projects retain their current output.
- Dynamic documentation values are normalized to one line and `*/` is neutralized, preventing
  source-controlled comment termination or multiline injection.
- The shared formatter reads only immutable validated `TargetModel` provenance, sorted effective
  decisions, and sorted applied decision identifiers.
- `JavaEmitter` modifies only `.java` artifacts and inserts documentation at the declaration
  boundary; other artifacts pass through unchanged.
- `DefaultNodeRenderer` modifies only planned per-program `.ts` artifacts. Project bootstrap and
  manifests remain program-independent, preserving multi-program deduplication.
- No COBOL IR, semantic IR, `TargetEmitter` SPI, executable source statement, or decision transition
  changed. Neither target emitter gained a COBOL-provider dependency.

## Residual scope

This work closes the target-emission seam that F6 explicitly deferred. It does not create a new LLM
prompt for explanatory prose. Any future LLM-produced explanation must first use the existing
validated, cached, human-reviewed annotation path; direct model prose remains prohibited from source
emission.
