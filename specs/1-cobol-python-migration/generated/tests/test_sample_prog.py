import os
import sys
import pytest
# Ensure the specs/1-cobol-python-migration directory is on sys.path so 'generated' package is importable
here = os.path.dirname(__file__)
spec_dir = os.path.abspath(os.path.join(here, '..', '..'))
if spec_dir not in sys.path:
    sys.path.insert(0, spec_dir)


def test_sample_prog_basic():
    try:
        from generated.sample_prog import run_sample_prog
    except ModuleNotFoundError:
        pytest.skip("generated.sample_prog not found; generator must run before this test")
    input_record = {"FIELD1": "ABC", "AMOUNT": "12345"}
    out = run_sample_prog(input_record)
    assert out["FIELD1"] == "ABC"
    assert out["AMOUNT"] == "12345"
