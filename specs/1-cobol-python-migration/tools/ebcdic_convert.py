#!/usr/bin/env python3
"""Utilities to convert EBCDIC <-> UTF-8 using code page CP037.
This is a simple helper for test fixtures and small files.

Functions:
- ebcdic_to_utf8(bytes_in) -> str
- utf8_to_ebcdic(text) -> bytes
"""
import codecs
from typing import Union


def ebcdic_to_utf8(b: Union[bytes, bytearray]) -> str:
    if not isinstance(b, (bytes, bytearray)):
        raise TypeError('Input must be bytes or bytearray')
    return b.decode('cp037')


def utf8_to_ebcdic(s: str) -> bytes:
    if not isinstance(s, str):
        raise TypeError('Input must be str')
    return s.encode('cp037')


if __name__ == '__main__':
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument('--infile', required=True)
    p.add_argument('--outfile', required=True)
    p.add_argument('--mode', choices=['to-utf8','to-ebcdic'], default='to-utf8')
    args = p.parse_args()

    if args.mode == 'to-utf8':
        with open(args.infile, 'rb') as f:
            data = f.read()
        text = ebcdic_to_utf8(data)
        with open(args.outfile, 'w', encoding='utf-8') as o:
            o.write(text)
    else:
        with open(args.infile, 'r', encoding='utf-8') as f:
            txt = f.read()
        data = utf8_to_ebcdic(txt)
        with open(args.outfile, 'wb') as o:
            o.write(data)

