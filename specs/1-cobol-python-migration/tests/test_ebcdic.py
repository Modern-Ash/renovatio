from specs_1_cobol_python_migration.tools.ebcdic_convert import ebcdic_to_utf8, utf8_to_ebcdic


def test_ebcdic_roundtrip():
    sample = 'HELLO WORLD 123'
    b = utf8_to_ebcdic(sample)
    assert isinstance(b, (bytes, bytearray))
    s = ebcdic_to_utf8(b)
    assert s == sample

