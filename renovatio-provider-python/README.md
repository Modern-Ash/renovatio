# Renovatio Python Lab Package

`renovatio-provider-python` is an unsupported lab package kept for COBOL
runtime/PIC research. It is not a production target provider.

Current scope:

- Python mirror of selected COBOL runtime behavior, currently `PicClause`.
- Focused pytest coverage for the runtime mirror.
- Historical context for a possible future Python target.

Not supported:

- No Java `TargetEmitter` implementation.
- No integration with the Maven reactor.
- No COBOL-to-Python artifact generation.
- No CLI, API, MCP or Workbench apply path.
- No equivalence gate for generated Python.

Run the lab tests:

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -e ".[test]"
pytest
```

Any future promotion to a supported target must implement the same canonical
pipeline, contract tests, packaging, fixtures, deterministic manifest and
equivalence gates required of Java and Node emitters.
