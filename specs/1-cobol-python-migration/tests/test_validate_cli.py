import json
from specs_1_cobol_python_migration.tools.validate import compare_dicts
from decimal import Decimal


def test_compare_exact():
    a={'FIELD1':'ABC','AMOUNT':'100.00'}
    b={'FIELD1':'ABC','AMOUNT':'100.00'}
    errs=compare_dicts(a,b,Decimal('0'))
    assert errs==[]


def test_compare_tolerance():
    a={'AMOUNT':'100.05'}
    b={'AMOUNT':'100.00'}
    errs=compare_dicts(a,b,Decimal('0.1'))
    assert errs==[]


def test_compare_fail():
    a={'AMOUNT':'101.00'}
    b={'AMOUNT':'100.00'}
    errs=compare_dicts(a,b,Decimal('0.1'))
    assert len(errs)>0

