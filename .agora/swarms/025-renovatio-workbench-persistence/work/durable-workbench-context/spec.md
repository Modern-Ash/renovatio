# Theia 4 — Persistencia de contexto de Workbench

## Outcome

Persist the selected Renovatio project and permitted presentation context so a Workbench restart
returns the user to a useful, project-scoped view without relying solely on browser local storage.

## Boundaries

- The Spring Boot API is the durable authority; Theia may retain a local cache only as a recovery aid.
- Stored data is limited to project identifier, active Renovatio area and selected asset identifier.
- No identity, session, role, credential, tenant secret, absolute workspace path or file content is
  persisted by this increment.
- The existing role gate remains authoritative. The temporary development no-auth bypass does not
  become a persistence or production authorization mechanism.
- Invalid, deleted or unauthorized project state resolves to the existing accessible empty or
  permission-denied state.
- Dashboard and wizard internals remain untouched.

## Completion conditions

1. A documented, project-scoped backend contract stores and retrieves the allowed context.
2. A restart restores valid state only after the live-project authorization boundary succeeds.
3. The UI has explicit safe recovery for missing, malformed, stale and denied state.
4. Automated and local integration evidence covers persistence, recovery and non-sensitive scope.
