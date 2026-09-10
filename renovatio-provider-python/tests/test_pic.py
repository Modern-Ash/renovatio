"""Mirror of renovatio-cobol-runtime PicClauseTest (Java)."""
from renovatio_python.cobol_runtime.pic import PicClause, Category, Usage


def test_parses_signed_packed_decimal_with_scale():
    t = PicClause.parse("S9(4)V99 COMP-3")
    assert t.category is Category.NUMERIC
    assert t.digits == 6
    assert t.scale == 2
    assert t.integer_digits == 4
    assert t.signed is True
    assert t.usage is Usage.COMP_3


def test_parses_unsigned_display_integer():
    t = PicClause.parse("PIC 9(5)")
    assert t.category is Category.NUMERIC
    assert t.digits == 5
    assert t.scale == 0
    assert t.signed is False
    assert t.usage is Usage.DISPLAY


def test_parses_alphanumeric_with_explicit_length():
    t = PicClause.parse("PIC X(30)")
    assert t.category is Category.ALPHANUMERIC
    assert t.digits == 30
    assert t.usage is Usage.DISPLAY


def test_parses_binary_comp():
    t = PicClause.parse("9(9) COMP")
    assert t.usage is Usage.COMP
    assert t.digits == 9


def test_parses_native_binary_comp_five():
    t = PicClause.parse("S9(9) USAGE COMP-5")
    assert t.category is Category.NUMERIC
    assert t.usage is Usage.COMP_5
    assert t.signed is True


def test_parses_separate_sign_clause():
    t = PicClause.parse("9(4) SIGN IS LEADING SEPARATE CHARACTER")
    assert t.category is Category.NUMERIC
    assert t.digits == 4
    assert t.signed is True


def test_parses_literal_nine_run():
    t = PicClause.parse("PIC 999V99")
    assert t.digits == 5
    assert t.scale == 2
