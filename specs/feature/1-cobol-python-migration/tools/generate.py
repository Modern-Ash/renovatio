#!/usr/bin/env python3
"""Simple generator: consume IR JSON and render Jinja2 templates to produce Python modules.

Usage:
  python generate.py --ir tmp/ir.json --templates templates/ --out generated/
"""
import argparse
import json
import os
from jinja2 import Environment, FileSystemLoader


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--ir", required=True)
    p.add_argument("--templates", required=True)
    p.add_argument("--out", required=True)
    args = p.parse_args()

    with open(args.ir, "r", encoding="utf-8") as f:
        ir = json.load(f)

    env = Environment(loader=FileSystemLoader(args.templates))
    template = env.get_template("program.py.j2")

    os.makedirs(args.out, exist_ok=True)
    init_path = os.path.join(args.out, "__init__.py")
    if not os.path.exists(init_path):
        open(init_path, "w", encoding="utf-8").write("# generated package\n")

    generated_paths = []
    for prog in ir.get("programs", []):
        name = prog.get("name")
        if not name:
            continue
        fname = f"{name}.py"
        out_path = os.path.join(args.out, fname)
        rendered = template.render(program=prog)
        with open(out_path, "w", encoding="utf-8") as out_f:
            out_f.write(rendered)
        generated_paths.append(out_path)

    # Print a simple JSON output compatible with the contract
    result = {"artifacts": generated_paths, "report": os.path.join(args.out, "report.md")}
    print(json.dumps(result))


if __name__ == '__main__':
    main()

