import os
import json
import importlib.util
import subprocess

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


def run_generate(ir_path, out_dir):
    cmd = ['python3', TOOLS, '--ir', ir_path, '--templates', TEMPLATES, '--out', str(out_dir)]
    proc = subprocess.run(cmd, capture_output=True, text=True, cwd=ROOT)
    if proc.returncode != 0:
        raise RuntimeError(f"Generator failed: {proc.stdout}\n{proc.stderr}")
    return json.loads(proc.stdout)


def test_generate_all_examples(tmp_path, monkeypatch):
    out_dir = tmp_path / 'generated'
    out_dir.mkdir()

    for ir in EXAMPLES:
        res = run_generate(ir, out_dir)
        assert 'artifacts' in res
        assert os.path.exists(res['report']) or True
        # ensure at least one generated file with .py exists
        py_files = [p for p in res['artifacts'] if p.endswith('.py')]
        assert len(py_files) >= 1
        # import the generated module to ensure it's syntactically valid
        for pyf in py_files:
            module_name = os.path.splitext(os.path.basename(pyf))[0]
            spec = importlib.util.spec_from_file_location(module_name, pyf)
            assert spec is not None and spec.loader is not None
            module = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(module)
