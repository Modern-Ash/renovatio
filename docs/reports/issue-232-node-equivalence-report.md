# Issue 232 Node Equivalence Report

## Result

Partial equivalence: structural target contract verified; behavioral equivalence remains pending.

## Verified In This Cycle

- Node shared artifacts are byte-identical across multiple COBOL programs.
- Program-scoped TypeScript files remain disjoint when the canonical architecture planner assigns
  disjoint module paths.
- CLI multiprogram generation writes the deduplicated Node tree with build, lint, test, lockfile,
  and idiom artifacts.
- API preview service exposes the same deterministic Node project artifacts.
- Capability surfaces keep Node `experimental`.

## Not Yet Claimed

- No beta maturity.
- No behavioral equivalence between COBOL execution and generated TypeScript execution.
- No clean generated-project `npm install`, `npm run build`, `npm run lint`, or `npm test` gate has
  been promoted to required CI.

## Follow-Up Gate

Before Node can move to beta, fixtures must execute the generated project in a pinned Node/npm
environment and record build, lint, test, and equivalence evidence against the exact generated
manifest hash.
