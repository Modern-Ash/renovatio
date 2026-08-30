from specs_1_cobol_python_migration.tools.comp3 import unpack_comp3, pack_comp3


def test_comp3_pack_unpack():
    # Example: number 12345.67 with digits=7 scale=2 => '01234567' digits incl frac
    s = '12345.67'
    digits = 7
    scale = 2
    packed = pack_comp3(s, digits, scale, negative=False)
    assert isinstance(packed, bytes)
    unpacked, neg = unpack_comp3(packed, digits, scale)
    assert neg == False
    assert unpacked == '12345.67'

