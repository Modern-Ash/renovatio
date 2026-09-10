#!/usr/bin/env python3
"""Simple copybook resolver for MVP.
Parses a minimal subset of COBOL copybook field declarations like:
   01 CUSTOMER-RECORD.
      05 CUST-NAME     PIC X(20).
      05 CUST-AMOUNT   PIC 9(7)V99.

Returns a list of field dicts: {name, pic, type, length, scale}
"""
import re
from typing import List, Dict

FIELD_RE = re.compile(
    r"^\s*\d+\s+([A-Z0-9\-]+)\s+PIC\s+"
    r"([X9]\([0-9]+\)(?:V(?:9\([0-9]+\)|[0-9]+))?)\.?",
    re.I,
)


def resolve_copybook(path: str) -> List[Dict]:
    fields = []
    if not path:
        return fields
    try:
        with open(path, 'r', encoding='utf-8') as f:
            for line in f:
                m = FIELD_RE.match(line)
                if m:
                    name = m.group(1).strip()
                    pic = m.group(2).strip().upper()
                    # crude parse
                    if pic.startswith('X'):
                        ftype = 'string'
                        length = int(re.search(r"\(([0-9]+)\)", pic).group(1))
                        scale = 0
                    else:
                        ftype = 'numeric'
                        m2 = re.search(r"9\(([0-9]+)\)(?:V(?:9\(([0-9]+)\)|([0-9]+)))?", pic)
                        if m2:
                            length = int(m2.group(1))
                            scale = int(m2.group(2)) if m2.group(2) else len(m2.group(3) or "")
                        else:
                            length = 0
                            scale = 0
                    fields.append({
                        'name': name,
                        'pic': pic,
                        'type': ftype,
                        'length': length,
                        'scale': scale
                    })
    except FileNotFoundError:
        return fields
    return fields


if __name__ == '__main__':
    import argparse
    p = argparse.ArgumentParser()
    p.add_argument('--copybook', required=True)
    args = p.parse_args()
    print(resolve_copybook(args.copybook))
