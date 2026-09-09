# Evidence: Python Build

- **Type:** build
- **Result:** success
- **Date:** 2026-09-08
- **Environment:** Ubuntu 24.04, Python 3.14.4

## renovatio-provider-python

- **Command:** `cd renovatio-provider-python && python3 -m venv .venv && source .venv/bin/activate && pip install -e ".[test]" && pytest`
- **Tests:** 7 passed, 0 failed

## Migration spec

- **Command:** `cd specs/1-cobol-python-migration && bash scripts/setup_env.sh && source .venv/bin/activate && pytest tests/`
- **Tests:** 9 passed, 0 failed
