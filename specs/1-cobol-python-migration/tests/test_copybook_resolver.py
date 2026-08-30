from specs_1_cobol_python_migration.tools.copybook_resolver import resolve_copybook
import os

def test_resolve_copybook(tmp_path):
    path = os.path.join(os.path.dirname(__file__), '..', 'examples', 'p1', 'copybooks', 'sample_copybook.cpy')
    fields = resolve_copybook(path)
    assert any(f['name']=='FIELD1' for f in fields)
    assert any(f['name']=='AMOUNT' for f in fields)
    amt = next(f for f in fields if f['name']=='AMOUNT')
    assert amt['type']=='numeric'
    assert amt['scale']==2

