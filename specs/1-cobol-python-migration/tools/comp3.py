#!/usr/bin/env python3
"""Simple COMP-3 (packed decimal) pack/unpack helpers.

This implements basic unpacking of packed decimal (IBM COMP-3) into Python Decimal-compatible string.
It's a minimal implementation intended for testing and not for production use.

Functions:
- unpack_comp3(packed_bytes, digits, scale) -> str (numeric string)
- pack_comp3(numeric_str, digits, scale) -> bytes

Note: signnybble is the last nibble: 0x0C/0x0F positive, 0x0D negative.
"""
from decimal import Decimal
from typing import Tuple


def _nibble_pairs(b: bytes):
    for byte in b:
        high = (byte >> 4) & 0x0F
        low = byte & 0x0F
        yield high
        yield low


def unpack_comp3(b: bytes, digits: int, scale: int) -> Tuple[str, bool]:
    """Unpack COMP-3 packed decimal to numeric string and sign.
    Returns (numeric_string_without_decimal_point, is_negative)
    digits: total digits (excluding sign nibble)
    scale: number of fractional digits
    """
    nibbles = list(_nibble_pairs(b))
    # last nibble is sign
    sign_nibble = nibbles[digits]
    is_negative = (sign_nibble == 0x0D)
    digits_vals = nibbles[:digits]
    s = ''.join(str(d) for d in digits_vals)
    if scale:
        int_part = s[:-scale] if len(s) > scale else '0'
        frac_part = s[-scale:].rjust(scale, '0')
        return (f"{int_part}.{frac_part}", is_negative)
    return (s, is_negative)


def pack_comp3(numeric_str: str, digits: int, scale: int, negative: bool=False) -> bytes:
    # remove decimal point
    if '.' in numeric_str:
        int_part, frac = numeric_str.split('.')
    else:
        int_part, frac = numeric_str, ''
    frac = frac.ljust(scale, '0')[:scale]
    combined = (int_part + frac).rjust(digits, '0')[-digits:]
    # build nibbles
    nibbles = [int(ch) for ch in combined]
    # append sign nibble
    sign = 0x0D if negative else 0x0C
    nibbles.append(sign)
    # pack into bytes
    out = bytearray()
    for i in range(0, len(nibbles), 2):
        high = nibbles[i]
        low = nibbles[i+1]
        out.append((high << 4) | (low & 0x0F))
    return bytes(out)


if __name__ == '__main__':
    print('COMP-3 helper')

