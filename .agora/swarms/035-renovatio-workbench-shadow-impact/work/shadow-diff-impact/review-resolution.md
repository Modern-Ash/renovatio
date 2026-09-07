# Review resolution

The three inline review threads on PR #192 were addressed and resolved:

- P1: existing targets are now scanned relative to the configured output root.
- P1: unmatched artifacts no longer inherit all domain evidence or source refs.
- P2: stale Shadow responses are discarded after project switches.

After the fix commit `c0c5e112180c755931250af7513b0be6c04648a5`, all review
threads were resolved through GitHub GraphQL and the PR reached `CLEAN` merge
state before merge.

