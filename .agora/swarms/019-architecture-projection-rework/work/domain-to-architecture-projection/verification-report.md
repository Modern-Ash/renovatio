# Verification report

## Result

PASS. `ArchitectureModel` v1 and `DomainArchitectureProjector` provide deterministic projections from the confirmed `DomainModel` for Transaction Script, Layered MVC, and Hexagonal styles. COBOL and Semantic IR are not read by the projector.

## Checks

- `mvn -q -pl renovatio-architecture -am test` — PASS.
- `git diff --check` — PASS.
- Focused test covers style-specific mapping and repeatability.
- Commit: `ec71c248d7559a95559f4db3131d4a85fbce887e`.

## Limitations

The model is the target-neutral contract. Existing emitters/layout adapters are not yet wired to consume this new projection; that is the next integration gap.
