# Temporary development authorization model

## Scope

Until `workbench-identity-login` delivers session and tenant enforcement, live-project writes are
permitted only in a local development profile with an explicit server-side flag. The default is deny.

## Guardrails

- The server owns the development-mode decision; the browser cannot enable it through a request.
- Read access and write access use the existing Spring Boot role model. Development configuration may
  supply the temporary role for local-only work, but production configuration must not do so.
- Legacy inputs (`.cbl`, `.cob`, `.cpy`, `.jcl`) are immutable regardless of temporary role.
- Writes are constrained below the configured project's approved generated-target roots and reject
  traversal or an unrecognized target type.
- The UI shows an explicit development-write banner and treats authorization failures as named states.
- Production/release deployment is blocked on the identity/login issue, including attributable audit
  records for mutations.
