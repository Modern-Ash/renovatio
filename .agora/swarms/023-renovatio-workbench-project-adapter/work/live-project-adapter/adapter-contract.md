# Live-project adapter contract

Theia consumes a dedicated Spring Boot Workbench adapter under `/api/projects/{projectId}/workbench`.
It returns project metadata plus categorized asset descriptors; it never returns arbitrary host paths.

## Read operations

- `GET /api/projects` lists visible projects.
- `GET /api/projects/{projectId}/workbench/assets` returns COBOL, copybooks, JCL, models, runs,
  evidence and generated-target asset descriptors.
- `GET /api/projects/{projectId}/workbench/assets/{assetId}` returns bounded asset content or a
  stable error response.

## Development write operation

`PUT /api/projects/{projectId}/workbench/assets/{assetId}` is available only when the server enables
the explicit development write mode. It accepts Java, Python, Node and other configured generated
target assets; COBOL, copybooks and JCL are rejected as read-only.

The API maps 401/403, 404 and failure responses to the Workbench's named permission, empty and error
states. The browser receives logical asset identifiers rather than raw filesystem paths.
