# Persistence contract

`GET /api/projects/{projectId}/workbench/context` returns an optional context with only
`activeArea` and `selectedAssetId`. `PUT` to the same URI accepts those two fields after project
authorization succeeds. The project id remains in the URI and is never copied into the body.

Allowed areas are `project`, `analysis`, `architecture`, `ai`, and `equivalence`. Asset ids must be
relative, non-empty paths without traversal. Invalid stored values are discarded on read and return
an empty context. This is project-scoped shared development state until the deferred identity issue
provides a user/tenant subject; it must not be represented as a personal preference.
