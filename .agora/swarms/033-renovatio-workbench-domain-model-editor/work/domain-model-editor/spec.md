# Theia 3 · Business DomainModel view and editor

## Outcome

The Renovatio Workbench SHALL expose a dedicated **Domain** area where a
modernization engineer can inspect, correct, validate, version and compare the
project's neutral `DomainModel` without regenerating target files or selecting a
target architecture.

The increment restores the already-completed #169 neutral DomainModel contract
from its three self-contained local commits because that closed dependency is
not present on `main`. No other unmerged decision-engine, dashboard or
architecture changes are part of this work.

## Model surface

The editor SHALL display entities, value objects, aggregates, use cases, domain
services, repositories, external systems, events, bounded contexts and business
invariants. A selected node SHALL expose its stable id, kind, name, structured
properties, origin, confidence and evidence. A selected relation SHALL expose
its endpoints, relation kind and cardinality.

Properties, node names, node kinds, relationship endpoints/kinds and
cardinality SHALL be editable through labelled structured controls. Stable ids,
project id, evidence source references and recorded origin SHALL remain
read-only in this increment.

## Provenance and navigation

Every node and invariant SHALL display its COBOL provenance: workspace-relative
source reference, parser provenance/rationale, content hash when available and
confidence. Missing evidence SHALL be an explicit validation diagnostic.

Selecting a source reference SHALL navigate to the Project Source Explorer and
select the referenced file. Selecting a Source Explorer symbol SHALL surface
matching DomainModel nodes whose evidence references that file/symbol. The
navigation is a deterministic reference match; it SHALL NOT run analysis.

## Validation and persistence

The backend SHALL persist immutable, monotonically numbered project versions.
Saving requires the currently loaded revision and creates a new version only
when the canonical model hash changes. Concurrent or stale saves SHALL return a
conflict without overwriting the newer version.

The save boundary SHALL reject blank/duplicate ids, unsupported schema
versions, relations or invariants referencing unknown nodes, duplicate property
names within a node and invalid cardinalities. Missing evidence and low
confidence are warnings visible before save but do not prevent a human-origin
correction from being versioned.

The user SHALL be able to list versions, restore a historical version as a new
version, and compare any two versions through a structured added/removed/changed
summary. History SHALL never be rewritten.

## Governed suggestions

Nodes and invariants with `origin=LLM` SHALL appear in a suggestion queue until
the user records one of `accepted`, `edited` or `rejected`. The decision SHALL
record suggestion id, action, resulting model revision and timestamp. Rejecting
a node that is still referenced SHALL fail validation; editing creates a new
model version. No suggestion is applied automatically.

## Workbench behavior

The area SHALL expose explicit `loading`, `ready`, `empty`,
`permission-denied`, `conflict` and `error` states. All editing controls SHALL be
keyboard operable, labelled, preserve visible focus and announce save,
validation and suggestion outcomes through an `aria-live` region. Motion SHALL
respect `prefers-reduced-motion` and the existing Control Deck visual language.

## Security and non-goals

Reads use the existing Workbench view authorization. Saves, restores and
suggestion decisions require the existing modify authorization, except for the
already-configured local development bypass. All operations are scoped to an
existing project id.

This increment SHALL NOT regenerate target code, choose MVC/Hexagonal/Clean,
modify source files, trigger analysis, invent evidence, merge change sets or
replace the current dashboard workflow.

## Verification

Verification SHALL cover model validation and canonical hashing, immutable
version persistence and optimistic conflicts, structured diffs, provenance
navigation, suggestion triage, API authorization, frontend contract behavior,
and an edit/save/reload smoke path.
