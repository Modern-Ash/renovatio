#!/usr/bin/env python3
"""Simple generator: consume IR JSON and render Jinja2 templates to produce Python modules.

Usage:
  python generate.py --ir tmp/ir.json --templates templates/ --out generated/

Enhancements:
- Logging
- Optional JSON Schema validation (--schema)
- Error handling and readable report.md output
"""
import argparse
import json
import os
import sys
import logging
from jinja2 import Environment, FileSystemLoader, TemplateError

try:
    import jsonschema
    HAS_JSONSCHEMA = True
except Exception:
    HAS_JSONSCHEMA = False

logger = logging.getLogger("cobol_python_generator")
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")


def validate_ir(ir: dict, schema_path: str):
    if not HAS_JSONSCHEMA:
        logger.warning("jsonschema not installed; skipping IR validation")
        return []
    if not os.path.exists(schema_path):
        logger.warning("IR schema not found at %s; skipping validation", schema_path)
        return []
    with open(schema_path, 'r', encoding='utf-8') as f:
        schema = json.load(f)
    validator = jsonschema.Draft7Validator(schema)
    errors = []
    for err in validator.iter_errors(ir):
        msg = f"{list(err.path)}: {err.message}"
        errors.append(msg)
    return errors


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--ir", required=True)
    p.add_argument("--templates", required=True)
    p.add_argument("--out", required=True)
    p.add_argument("--schema", required=False, help="Optional path to IR JSON Schema")
    args = p.parse_args()

    # determine default schema path if not provided
    if args.schema:
        schema_path = args.schema
    else:
        # expect contracts/ir-schema.json next to the parent of templates
        parent = os.path.abspath(os.path.join(args.templates, os.pardir))
        schema_path = os.path.join(parent, 'contracts', 'ir-schema.json')

    try:
        with open(args.ir, "r", encoding="utf-8") as f:
            ir = json.load(f)
    except Exception as e:
        logger.error("Failed to read IR file %s: %s", args.ir, e)
        sys.exit(2)

    # validate IR
    validation_errors = validate_ir(ir, schema_path)
    if validation_errors:
        logger.error("IR validation failed with %d errors", len(validation_errors))
        for e in validation_errors:
            logger.error("  - %s", e)
        # continue but emit in report

    env = Environment(loader=FileSystemLoader(args.templates), keep_trailing_newline=True)
    try:
        template = env.get_template("program.py.j2")
    except TemplateError as e:
        logger.error("Failed to load template: %s", e)
        sys.exit(3)

    os.makedirs(args.out, exist_ok=True)
    init_path = os.path.join(args.out, "__init__.py")
    if not os.path.exists(init_path):
        open(init_path, "w", encoding="utf-8").write("# generated package\n")

    generated_paths = []
    warnings = []
    errors = []

    for prog in ir.get("programs", []):
        name = prog.get("name")
        if not name:
            warnings.append("Skipping program with no name")
            continue
        fname = f"{name}.py"
        out_path = os.path.join(args.out, fname)
        try:
            rendered = template.render(program=prog)
        except Exception as e:
            msg = f"Template rendering failed for program {name}: {e}"
            logger.exception(msg)
            errors.append(msg)
            continue
        try:
            with open(out_path, "w", encoding="utf-8") as out_f:
                out_f.write(rendered)
            generated_paths.append(out_path)
            logger.info("Wrote %s", out_path)
        except Exception as e:
            msg = f"Failed to write output file {out_path}: {e}"
            logger.exception(msg)
            errors.append(msg)

    # additionally, render dataclasses models if template exists
    try:
        models_tpl = env.get_template('dataclass.py.j2')
        # render using first program for records (MVP)
        if ir.get('programs'):
            models_rendered = models_tpl.render(program=ir['programs'][0])
            models_path = os.path.join(args.out, 'models.py')
            with open(models_path, 'w', encoding='utf-8') as m:
                m.write(models_rendered)
            generated_paths.append(models_path)
            logger.info('Wrote models %s', models_path)
            # write mapping
            mapping_path = os.path.join(args.out, 'mapping.md')
            with open(mapping_path, 'w', encoding='utf-8') as mp:
                mp.write('# Mapping report\n\n')
                for rec in ir['programs'][0].get('records', []):
                    mp.write(f"- Record: {rec.get('name')}\n")
                    for fld in rec.get('fields', []):
                        mp.write(f"  - {fld.get('name')}: {fld.get('pic')}\n")
            logger.info('Wrote mapping %s', mapping_path)
    except Exception:
        logger.debug('No dataclass template found or failed to render models')

    report_path = os.path.join(args.out, "report.md")
    try:
        with open(report_path, 'w', encoding='utf-8') as r:
            r.write("# Migration generation report\n\n")
            r.write(f"IR file: {args.ir}\n\n")
            if validation_errors:
                r.write("## IR validation errors\n")
                for e in validation_errors:
                    r.write(f"- {e}\n")
                r.write('\n')
            if warnings:
                r.write("## Warnings\n")
                for w in warnings:
                    r.write(f"- {w}\n")
                r.write('\n')
            if errors:
                r.write("## Errors\n")
                for e in errors:
                    r.write(f"- {e}\n")
                r.write('\n')
            r.write("## Artifacts\n")
            for pth in generated_paths:
                r.write(f"- {pth}\n")
            r.write('\n')
        logger.info("Report written to %s", report_path)
    except Exception as e:
        logger.error("Failed to write report: %s", e)

    # Print a simple JSON output compatible with the contract
    result = {"artifacts": generated_paths, "report": report_path, "warnings": warnings, "errors": errors}
    print(json.dumps(result))


if __name__ == '__main__':
    main()
