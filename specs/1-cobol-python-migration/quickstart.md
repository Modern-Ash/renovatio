# Quickstart — COBOL → Python migration PoC

These are the basic steps to run the migration PoC locally.

Prerequisites
- Python 3.11+
- Bash (for stub scripts)

Setup

```bash
# Setup virtualenv and install requirements
bash specs/1-cobol-python-migration/scripts/setup_env.sh
source specs/1-cobol-python-migration/.venv/bin/activate
```

Run extractor (stub)

```bash
# Run extractor stub: produces an IR JSON from a COBOL program
specs/1-cobol-python-migration/tools/extractor_stub.sh --cobol specs/1-cobol-python-migration/examples/p1/prog1.cob --out specs/1-cobol-python-migration/tmp/ir.json
```

Run generator

```bash
python3 specs/1-cobol-python-migration/tools/generate.py \
  --ir specs/1-cobol-python-migration/tmp/ir.json \
  --templates specs/1-cobol-python-migration/templates \
  --out specs/1-cobol-python-migration/generated
```

Validate generated outputs

```bash
# If generator produces a JSON output to validate (or you can exercise the generated module directly)
python3 specs/1-cobol-python-migration/tools/validate.py --generated GENERATED_JSON --golden specs/1-cobol-python-migration/examples/p1/golden/prog1.out --tol 0.01
```

Notes
- The `extractor_stub.sh` is a placeholder that returns an example IR; replace it with your real COBOL extractor when available.
- Use `specs/1-cobol-python-migration/tools/generate.py --schema path/to/schema.json` to validate IR while generating (requires `jsonschema` in your venv).
