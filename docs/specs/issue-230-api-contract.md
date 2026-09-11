# Issue 230 API Contract

## Endpoint

`GET /api/v1/capabilities`

Returns the shared `renovatio.surface-capabilities` document.

## CLI

`renovatio capabilities`

Human output lists the contract id/version and each capability with maturity.

`renovatio capabilities --json`

JSON output is the same contract document returned by the API.

## MCP

Tool: `renovatio_capabilities`

No input arguments are required. The structured result includes the same contract document plus `success=true`.
For compatibility, execution also accepts the legacy alias `renovatio.capabilities`; `tools/list` and
`tools/describe` advertise the normalized MCP name.
