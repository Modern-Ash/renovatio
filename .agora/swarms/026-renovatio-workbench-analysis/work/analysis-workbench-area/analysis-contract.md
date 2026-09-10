# Analysis contract

`GET /api/projects/{projectId}/workbench/analysis` returns category counts from the authorized
workspace plus persisted run summaries. It has no write methods and uses the existing view gate.
