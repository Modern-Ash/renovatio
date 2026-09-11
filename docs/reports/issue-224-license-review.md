# Issue #224 License Review

Date: 2026-09-11

## Status

Resolved. The project owner approved the MIT License on 2026-09-11.

## Recommendation

Apache-2.0 is recommended for Renovatio because it is permissive and includes an
explicit patent grant, which is helpful for a modernization engine with
potentially reusable architecture and code-generation components.

MIT is also viable if the owner prefers a shorter permissive license and accepts
the lack of an explicit patent grant. The owner selected MIT for this
repository.

## Current Repository State

- Root `LICENSE` now exists with standard MIT terms and copyright holder
  `Modern Ash`.
- Community contribution policy now uses DCO sign-off.
- No CLA is introduced.

## Decision

The license decision is complete for issue #224. Future release candidates
should still attach generated Maven/npm/Python dependency license reports before
publication.

## Dependency Compatibility Notes

The project uses common permissive/open-source ecosystems: Spring Boot, Maven,
OpenRewrite, React/Vite, Theia-related packages, pytest, Jinja2 and jsonschema.
A preliminary inventory should be treated as release-readiness evidence, not a
substitute for legal review.
