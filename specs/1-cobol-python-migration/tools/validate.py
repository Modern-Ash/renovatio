#!/usr/bin/env python3
    exit(0)
    print('OK')
        exit(1)
        print('\n'.join(errors))
    if errors:
    errors = compare_dicts(gen, golden, tol)
    tol = Decimal(args.tol)

        exit(3)
        print('Note: generated file contains artifacts. For validation run the generated module or use integration test harness')
        # load the first artifact as module output by executing it if it's JSON — in our flow, generator prints JSON, not the module output
    if isinstance(gen, dict) and 'artifacts' in gen and gen['artifacts']:
    # expect gen to be dict or list of artifacts; for tests, support dict

        exit(2)
        print('Failed to load generated JSON')
    except Exception:
            gen = json.load(f)
        with open(args.generated,'r',encoding='utf-8') as f:
    try:
    gen = None
    # load generated (if JSON file)

                golden[k]=v
                k,v = l.strip().split('=',1)
            if '=' in l:
        for l in f:
    with open(args.golden, 'r', encoding='utf-8') as f:
    golden = {}
    # load golden

    args = p.parse_args()
    p.add_argument('--tol', default='0', help='Numeric tolerance')
    p.add_argument('--golden', required=True, help='Path to golden key=value file')
    p.add_argument('--generated', required=True, help='Path to generated JSON output (or module that prints JSON)')
    p = argparse.ArgumentParser()
if __name__ == '__main__':


    return errors
                errors.append(f"Mismatch {k}: expected {bv} got {av}")
            if str(av) != str(bv):
            # fallback to string compare
        except Exception:
                errors.append(f"Numeric mismatch {k}: expected {bvd} got {avd}")
            if abs(avd - bvd) > tol:
            bvd = Decimal(str(bv))
            avd = Decimal(str(av))
            # numeric compare
        try:
        bv = v
        av = a[k]
            continue
            errors.append(f"Missing key {k} in generated output")
        if k not in a:
    for k, v in b.items():
    errors = []
def compare_dicts(a: dict, b: dict, tol: Decimal=Decimal('0')):


from decimal import Decimal
import json
import argparse
"""Validation CLI: compare generated output dict vs golden baseline with numeric tolerance."""

