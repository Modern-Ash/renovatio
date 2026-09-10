# Review policy: idiomatic polish proposals

> GitHub issue: [#128](https://github.com/Modern-Ash/renovatio/issues/128)  
> Agora work: `ai-modernization/idiomatic-polish-proposals`

## Purpose

Define the human-only decision boundary for inert idiomatic-polish artifacts. Automated generation
and validation may make a proposal eligible for review; neither an LLM nor any automated component
may accept, apply, commit, merge, or deploy it.

## Authority

The proposal artifact always starts in `PROPOSED`. Only the human `project:owner` may record an
accept or reject decision, and that decision occurs in separate governed work outside the proposal
generation request. Acceptance of this policy or of the issue implementation does not accept any
individual patch.

## Required review package

A reviewer receives one immutable proposal directory containing `proposal.patch` and
`manifest.json`, plus the referenced test report and action-item report. Review must stop if any
artifact hash differs from the manifest or if the evidence is not bound to the current commit and
deterministic baseline.

## Human checklist

The reviewer confirms all of the following before accepting a proposal:

- the proposal family and affected IR nodes match the requested modernization outcome;
- every changed path is declared, beneath the generated-Java root, and present in the manifest;
- the patch contains no COBOL, IR, sidecar, schema, prompt, test, fixture, configuration, build,
  report, repository-metadata, absolute, parent-traversal, or symbolic-link escape path;
- every input, output, patch, schema, prompt, baseline, and evidence hash matches;
- the exact affected characterization selectors were rerun against the candidate;
- Java 17 compilation and characterization results are green and current;
- repeated generation produced byte-identical patch and manifest artifacts;
- public-signature changes are individually listed with compatibility impact and explicit owner
  disposition;
- the family-specific constraints in the accepted specification are satisfied;
- provenance contains no credential, raw provider envelope, hidden reasoning, or unrelated source;
- deterministic generated Java remained unchanged during generation and validation;
- no automatic apply path, workspace writer, commit, merge, or deployment was invoked.

Any missing, stale, contradictory, or unverifiable item requires rejection. Persuasive prose,
confidence, compilation alone, or a cache hit is never sufficient.

## Decision records

An individual decision references the proposal ID and immutable manifest hash and records:

- `ACCEPTED` or `REJECTED`;
- reviewer identity `project:owner`;
- RFC 3339 UTC decision time;
- concise rationale;
- any separately governed follow-up work that may apply the patch.

The generation service cannot write this decision into the proposal artifact. A rejected proposal
leaves deterministic output unchanged. An accepted proposal also leaves it unchanged until a
separate authorized workflow applies the patch.

## Failure and revocation

If evidence becomes stale, a referenced baseline changes, a hash no longer matches, or a later
guardrail fails, prior review eligibility is revoked. The proposal must be regenerated and reviewed
as a new content-addressed proposal; its payload or identity cannot be edited in place.

