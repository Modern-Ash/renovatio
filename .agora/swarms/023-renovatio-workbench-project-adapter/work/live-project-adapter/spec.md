# Specification — Theia 2: authorized live-project adapter

## Objective

Replace the deterministic Renovatio Workbench project fixture with an adapter to the existing Spring
Boot backend. The adapter exposes project metadata and the agreed asset inventory without importing
wizard internals or changing the existing dashboard flow.

## Confirmed decisions

- Spring Boot is the authoritative source for projects, COBOL sources, copybooks, JCL, models,
  runs and evidence.
- This increment exposes the complete listed inventory through a documented Workbench adapter
  contract.
- COBOL sources and related legacy inputs are read-only.
- Generated or modern target assets, including Java, Python and Node, may be written in a temporary
  development mode without authentication.
- The temporary mode is disabled by default, must be conspicuously labelled in the UI and is
  restricted to the configured development workspace. It is prohibited in production and release
  environments.
- Identity, login, session propagation, tenant authorization and mutation auditing are deferred to
  `workbench-identity-login` and must be completed before any production write capability.

## Boundaries

- The adapter does not import, embed or replace dashboard or wizard internals.
- It does not add a new authentication protocol or silently fall back to anonymous production use.
- It must expose explicit loading, empty, permission-denied and error states.
- It must reject COBOL/legacy write attempts deterministically.

## Acceptance evidence

The implementation must include an adapter contract, authorization-mode documentation, integration
tests against a Spring Boot-compatible boundary, write-policy tests for asset types and a technical
review. The identity issue is a release dependency, not an implementation substitute.
