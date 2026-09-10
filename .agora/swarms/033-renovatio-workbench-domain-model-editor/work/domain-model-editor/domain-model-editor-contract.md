# DomainModel editor contract

## Neutral model additions

`renovatio-domain-model` retains schema version `1` and deterministic ordering.
`DomainNode` adds `properties`, where each property contains `name`, `type`,
`required` and optional evidence. `DomainRelation` adds `sourceCardinality` and
`targetCardinality`, each one of `ONE`, `ZERO_OR_ONE`, `ONE_OR_MORE` or
`ZERO_OR_MORE`. Backward-compatible constructors and JSON defaults preserve
previous v1 payloads.

## Read model

`GET /api/projects/{projectId}/workbench/domain-model` returns:

```json
{
  "revision": 3,
  "canonicalHash": "sha256:...",
  "savedAt": "2026-09-07T12:00:00Z",
  "model": {
    "schemaVersion": "1",
    "projectId": "payroll-modernization",
    "nodes": [],
    "relations": [],
    "invariants": []
  },
  "diagnostics": [],
  "suggestions": []
}
```

A project with no saved model returns revision `0`, an empty neutral model and
an explicit `empty` representation; it does not synthesize business elements.

## Save and restore

`PUT /api/projects/{projectId}/workbench/domain-model` accepts:

```json
{ "expectedRevision": 3, "model": { "schemaVersion": "1", "projectId": "payroll-modernization" } }
```

The full `nodes`, `relations` and `invariants` arrays are required. Success
returns the new read model. A stale `expectedRevision` returns `409`; structural
validation failure returns `400` with diagnostics; unauthorized mutation
returns `403`.

`POST /api/projects/{projectId}/workbench/domain-model/versions/{revision}:restore`
accepts `{ "expectedRevision": 4 }` and saves the selected historical payload
as a new latest version.

## Versions and comparison

`GET /api/projects/{projectId}/workbench/domain-model/versions` returns version
metadata ordered newest first: revision, canonical hash and timestamp.

`GET /api/projects/{projectId}/workbench/domain-model/compare?from=2&to=4`
returns deterministic lists of added, removed and changed entries. Each entry
identifies `node`, `relation` or `invariant`, its stable id, and the before/after
JSON values when applicable.

## Suggestion triage

`POST /api/projects/{projectId}/workbench/domain-model/suggestions/{suggestionId}`
accepts:

```json
{
  "action": "accepted",
  "expectedRevision": 4,
  "editedNode": null
}
```

`action` is `accepted`, `edited` or `rejected`. `editedNode` is required only
for `edited`. Each successful action records an immutable decision and returns
the resulting read model; stale revision is `409` and a rejected referenced node
is `400`. Repeating the same action for an already-decided suggestion is
idempotent; a different second action is a conflict.

## Source navigation

Evidence `sourceRef` uses a workspace-relative file id, optionally followed by
`#<symbol-id>` or `:<line>`. The frontend matches the file prefix against Source
Explorer ids and never resolves outside the selected project workspace.
