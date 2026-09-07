# Equivalence adapter contract

`GET /api/projects/{projectId}/workbench/equivalence` returns:

```json
{
  "evidence": [{ "id": "...", "name": "..." }],
  "generatedTargets": [{ "id": "...", "name": "..." }]
}
```

The endpoint is governed by the existing Workbench view authorization boundary
and never executes a comparator. An empty collection means no persisted
artifact of that kind is available; it does not mean equivalence passed.
