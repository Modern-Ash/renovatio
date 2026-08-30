import os
import subprocess
import json

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
IR = os.path.join(ROOT, '..', 'examples', 'p1', 'ir_prog1.json')
TEMPLATES = os.path.join(ROOT, '..', 'templates')
OUT = os.path.join(ROOT, '..', 'generated')
GOLDEN = os.path.join(ROOT, '..', 'examples', 'p1', 'golden', 'prog1.out')


def read_golden(path):
    with open(path, 'r', encoding='utf-8') as f:
        lines = [l.strip() for l in f.readlines() if l.strip()]
    d = {}
    for l in lines:
        if '=' in l:
            k,v = l.split('=',1)
            d[k]=v
    return d


def parse_output_module(module_path):
    # import generated module and try to run run_<prog>()
    import importlib.util
    spec = importlib.util.spec_from_file_location('genmod', module_path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    # find function starting with run_
    fn = None
    for name in dir(mod):
        if name.startswith('run_'):
            fn = getattr(mod, name)
            break
    if not fn:
        raise RuntimeError('No run_ function found in generated module')
    # call with sample record
    return fn({ 'FIELD1': 'ABCDEFGHIJ', 'AMOUNT': '12345' })


def test_end_to_end(tmp_path):
    # ensure clean out
    if os.path.exists(OUT):
        for f in os.listdir(OUT):
            os.remove(os.path.join(OUT,f))
    os.makedirs(OUT, exist_ok=True)

    # run generator
    cmd = ['python3', os.path.join(ROOT,'..','tools','generate.py'), '--ir', IR, '--templates', TEMPLATES, '--out', OUT]
    proc = subprocess.run(cmd, capture_output=True, text=True)
    assert proc.returncode == 0
    res = json.loads(proc.stdout)
    assert 'artifacts' in res
    py_files = [p for p in res['artifacts'] if p.endswith('.py')]
    assert len(py_files) >= 1

    # import and execute generated module
    out = parse_output_module(py_files[0])
    golden = read_golden(GOLDEN)
    assert out['FIELD1'] == golden['FIELD1']
    assert str(out['AMOUNT']) == golden['AMOUNT']

