# Issue #234 Python Capability Assessment

Date: 2026-09-11

## Inventory

Current files under `renovatio-provider-python`:

- `pyproject.toml`
- `src/renovatio_python/__init__.py`
- `src/renovatio_python/cobol_runtime/__init__.py`
- `src/renovatio_python/cobol_runtime/pic.py`
- `tests/test_pic.py`

Measured source/test size:

- Runtime package: 105 LOC across `pic.py`, package initializers.
- Tests: 54 LOC in `tests/test_pic.py`.
- Total: 159 LOC.

Dependencies:

- Runtime declares `jinja2>=3.0` and `jsonschema>=4.0`.
- Test extra declares `pytest>=7.0`.

Implemented capability:

- COBOL `PIC` parsing mirror for numeric, alphanumeric, alphabetic, signed and
  selected usage clauses.

## TargetEmitter Gap Matrix

| Requirement | Current State |
| --- | --- |
| Java `TargetEmitter` implementation | Missing |
| `supports(MigrationProfile.Language.PYTHON)` | Missing |
| `emit(TargetModel, MigrationProfile)` | Missing |
| Deterministic `EmittedArtifacts` manifest | Missing |
| Canonical application service routing | Missing |
| CLI/API/MCP/Workbench apply path | Missing |
| Contract tests equivalent to Java/Node emitters | Missing |
| Generated Python fixtures | Missing |
| Equivalence gate for generated output | Missing |
| Maven reactor module | Missing |
| CI coverage | Present only for lab pytest and Python audit |

## Conclusion

The package is useful research code, not a production provider. Keeping it as an
unsupported lab package preserves history while removing the false promise that
Python is a ready or planned target emitter.
