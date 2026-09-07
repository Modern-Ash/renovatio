# Demo pilot report

## Pilot

COBOL demo programs p1

## Fixture manifest

The pilot uses the immutable fixture list and hashes in `config/release-hardening.json`:

- `../specs/1-cobol-python-migration/examples/p1/prog1.cob`
- `../specs/1-cobol-python-migration/examples/p1/prog2.cob`
- `../specs/1-cobol-python-migration/examples/p1/prog3.cob`
- `../specs/1-cobol-python-migration/examples/p1/ir_prog1.json`
- `../specs/1-cobol-python-migration/examples/p1/ir_prog2.json`
- `../specs/1-cobol-python-migration/examples/p1/ir_prog3.json`
- `../renovatio-provider-cobol/src/test/resources/equivalence/equivalence-balance/input.cob`

## Pilot procedure

1. Start from a clean clone.
2. Run `npm ci` in `renovatio-workbench`.
3. Run `npm run build`.
4. Run `npm run pilot:demo` to verify demo COBOL/IR fixture hashes.
5. Run `npm run smoke` to prove the web distribution opens.
6. Open the workbench against the demo workspace and verify Analysis, Source Explorer, DomainModel, Architecture Canvas, Shadow Impact, AI, Change Sets, and Equivalence Lab still navigate without replacing the existing dashboard.

## Result gate

The pilot passes when fixture hashes match, the smoke endpoint serves the Theia shell, and the dashboard continuity link remains available.
