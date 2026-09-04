# F8 post-merge review corrections

## Context

PR #167 implemented issue #154 and was merged before five review findings were
addressed. This follow-up keeps the public F8 behavior while closing the data-loss,
compatibility, CLI integration, and stale-policy gaps identified by that review.

## Required behavior

### Local decisions remain authoritative

Applying a policy catalog must not change an active local decision whose status is
`CONFIRMED` or `OVERRIDDEN`. A decision is local when its source is not `POLICY`.
Policy-sourced confirmations may still be refreshed by an explicitly applied catalog.

### Unbound effective-profile identity remains compatible

The layered effective-profile resolver must produce the exact legacy F1 hash when
neither a template nor policy binding is present. Binding metadata participates in
the canonical hash only when at least one binding exists.

### CLI profile bindings affect execution

`profile apply` must retain the explicit template reference and materialize the
template plus current project overlay as the project's profile. The headless CLI
must resolve subsequent `plan` and `apply` generation from the project's local
`.renovatio/migration-profile.json` and `.renovatio/decisions.json` state. Plan replay
must keep using the original project state even when dry-run generation executes in
a temporary workspace.

### CLI analysis creates reusable decision state

A successful `analyze` must deterministically reconcile the seven F1 decision points
into `.renovatio/decisions.json`, preserving valid local confirmations and overrides.
The CLI must expose `decisions list` and `decisions set` so a user can review and
confirm or override those decisions without editing JSON. `policy export` continues
to export only confirmed/overridden decisions; its normal flow is therefore
`analyze`, `decisions set`, then `policy export`, with no test-only seed.

### Stale policies are reviewable

Semantic matching must still find a same-category, same-decision-key,
same-node-kind policy when its option vocabulary changed. A vocabulary change,
analyzer/schema change, or no-longer-valid selected option makes the match stale.
A stale match must never auto-confirm. If the selected option remains valid it is a
policy suggestion; if it was removed it leaves the current choice untouched but is
still reported as a stale suggestion with policy identity and confidence, rather
than as an ordinary unmatched decision.

## Compatibility and constraints

- Existing stored profile, decision, template, and policy JSON remains readable.
- No remote template registry, automatic binding updates, or RBAC is introduced.
- Deterministic defaults and current unbound cache identities remain unchanged.
- Existing API/UI contracts remain source compatible; stale removed-option matches
  use the existing match report rather than adding a required persisted field.
- All changes require focused regression tests plus the repository verification
  suites named in the work item.
