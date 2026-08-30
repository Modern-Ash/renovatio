import os
import ast
from jinja2 import Environment, FileSystemLoader
import json

BASE = os.path.dirname(__file__)
ROOT = os.path.abspath(os.path.join(BASE, '..'))
TEMPLATES = os.path.join(ROOT, 'templates')
EXAMPLE_IR = os.path.join(ROOT, 'examples', 'p1', 'ir_prog1.json')


def test_template_renders_and_compiles():
    env = Environment(loader=FileSystemLoader(TEMPLATES))
    tpl = env.get_template('program.py.j2')
    with open(EXAMPLE_IR, 'r', encoding='utf-8') as f:
        ir = json.load(f)
    for prog in ir.get('programs', []):
        rendered = tpl.render(program=prog)
        # ensure syntactically valid Python by parsing AST
        ast.parse(rendered)
        assert 'def run_' in rendered

