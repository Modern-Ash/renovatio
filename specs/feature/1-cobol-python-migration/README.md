# COBOL → Python Migration (MVP)

This spec directory contains a minimal generator and examples to validate the end-to-end flow for migrating simple COBOL programs to Python.

Quickstart

1. Create virtualenv:

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

2. Run generator for example IR:

```bash
python3 tools/generate.py --ir tmp/ir.json --templates templates --out generated
```

3. Run tests:

```bash
PYTHONPATH=. pytest -q
```

Notes
- This is an MVP generator intended for demonstration and testing. Production-grade features (COMP-3 handling, EBCDIC conversion, DB2 adapters) are not implemented yet.

