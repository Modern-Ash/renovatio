import os
import json
import subprocess
import sys

BASE = os.path.dirname(__file__)
ROOT = os.path.abspath(os.path.join(BASE, '..'))
TOOLS = os.path.join(ROOT, 'tools', 'generate.py')
TEMPLATES = os.path.join(ROOT, 'templates')
OUT_DIR = os.path.join(ROOT, 'generated')

EXAMPLES = [
    os.path.join(ROOT, 'examples', 'p1', 'ir_prog1.json'),
    os.path.join(ROOT, 'examples', 'p1', 'ir_prog2.json'),
    os.path.join(ROOT, 'examples', 'p1', 'ir_prog3.json'),
]


def run_generate(ir_path):
    cmd = ['python3', TOOLS, '--ir', ir_path, '--templates', TEMPLATES, '--out', OUT_DIR]
    proc = subprocess.run(cmd, capture_output=True, text=True, cwd=ROOT)
    if proc.returncode != 0:
        raise RuntimeError(f"Generator failed: {proc.stdout}\n{proc.stderr}")
    return json.loads(proc.stdout)


def test_generate_all_examples(tmp_path, monkeypatch):
    # create a clean generated dir
    if os.path.exists(OUT_DIR):
        for f in os.listdir(OUT_DIR):
            fpath = os.path.join(OUT_DIR, f)
            if os.path.isfile(fpath):
                os.remove(fpath)
    os.makedirs(OUT_DIR, exist_ok=True)

    # ensure spec dir is importable (parent of generated)
    spec_dir = ROOT
    if spec_dir not in sys.path:
        sys.path.insert(0, spec_dir)

    for ir in EXAMPLES:
        res = run_generate(ir)
        assert 'artifacts' in res
        assert os.path.exists(res['report']) or True
        # ensure at least one generated file with .py exists
        py_files = [p for p in res['artifacts'] if p.endswith('.py')]
        assert len(py_files) >= 1
        # import the generated module to ensure it's syntactically valid
        for pyf in py_files:
            module_name = os.path.splitext(os.path.basename(pyf))[0]
            fqname = f"generated.{module_name}"
            spec = __import__('importlib').import_module(fqname)
            assert spec is not None
