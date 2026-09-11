# Issue #224 License Review

Date: 2026-09-11

## Status

License decision pending owner approval.

## Recommendation

Apache-2.0 is recommended for Renovatio because it is permissive and includes an
explicit patent grant, which is helpful for a modernization engine with
potentially reusable architecture and code-generation components.

MIT is also viable if the owner prefers a shorter permissive license and accepts
the lack of an explicit patent grant.

## Current Repository State

- No root `LICENSE` file exists before this issue.
- Community contribution policy now uses DCO sign-off.
- No CLA is introduced.

## Decision Required

The owner must approve one explicit license before this acceptance criterion can
be fully closed. No license text has been added in this change because the issue
forbids choosing a license on behalf of the owner.

## Dependency Compatibility Notes

The project uses common permissive/open-source ecosystems: Spring Boot, Maven,
OpenRewrite, React/Vite, Theia-related packages, pytest, Jinja2 and jsonschema.
A preliminary inventory should be treated as release-readiness evidence, not a
substitute for legal review.
