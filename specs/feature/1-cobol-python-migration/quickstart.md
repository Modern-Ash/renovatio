# quickstart.md — Quickstart: Migración COBOL → Python (MVP)

Prerequisitos:
- Java 17+ (para usar ProLeap/Koopa si se reutiliza Java para parseo)
- Python 3.10+ (preferible) con virtualenv
- tools: iconv (opcional) para EBCDIC→UTF-8

1) Preparar entorno Python

```bash
python -m venv .venv
source .venv/bin/activate
pip install jinja2 pytest
```

2) Colocar ejemplo COBOL en `examples/p1/` (archivo .cob y copybooks)

3) Ejecutar extractor COBOL → IR (si existe implementacion Java):

```bash
# Ejemplo si existe un jar extractor
java -jar tools/cobol-extractor.jar --input examples/p1 --output tmp/ir.json
```

Si no hay extractor, crear `tmp/ir.json` manualmente siguiendo `specs/1-cobol-python-migration/data-model.md`.

4) Generar código Python usando jinja2 templates

```bash
python tools/generate.py --ir tmp/ir.json --templates templates/ --out generated/
```

5) Ejecutar tests

```bash
pytest generated/tests
```

6) Validar resultados: comparar outputs con `examples/p1/golden/`

7) Empaquetar artefacto (opcional)

```bash
pip install build
python -m build
```


Documentar problemas y puntos manuales en `generated/report.md`.

