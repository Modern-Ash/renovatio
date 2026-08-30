#!/usr/bin/env python3
"""Compare generated output with a golden baseline."""

import argparse
import json
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any


def compare_dicts(
    generated: dict[str, Any],
    golden: dict[str, Any],
    tolerance: Decimal = Decimal("0"),
) -> list[str]:
    """Return human-readable mismatches between generated and golden values."""
    errors: list[str] = []
    for key, expected in golden.items():
        if key not in generated:
            errors.append(f"Missing key {key} in generated output")
            continue

        actual = generated[key]
        try:
            actual_decimal = Decimal(str(actual))
            expected_decimal = Decimal(str(expected))
        except (InvalidOperation, ValueError):
            if str(actual) != str(expected):
                errors.append(f"Mismatch {key}: expected {expected} got {actual}")
            continue

        if abs(actual_decimal - expected_decimal) > tolerance:
            errors.append(f"Numeric mismatch {key}: expected {expected_decimal} got {actual_decimal}")

    return errors


def load_golden(path: Path) -> dict[str, str]:
    """Load a simple KEY=VALUE golden fixture."""
    values: dict[str, str] = {}
    with path.open(encoding="utf-8") as fixture:
        for line in fixture:
            stripped = line.strip()
            if stripped and "=" in stripped:
                key, value = stripped.split("=", 1)
                values[key] = value
    return values


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--generated", required=True, help="Path to generated JSON output")
    parser.add_argument("--golden", required=True, help="Path to golden KEY=VALUE fixture")
    parser.add_argument("--tol", default="0", help="Numeric tolerance")
    args = parser.parse_args()

    try:
        with Path(args.generated).open(encoding="utf-8") as generated_file:
            generated = json.load(generated_file)
    except (OSError, json.JSONDecodeError) as error:
        print(f"Failed to load generated JSON: {error}")
        return 2

    if not isinstance(generated, dict):
        print("Generated JSON must be an object")
        return 2
    if generated.get("artifacts"):
        print(
            "Generated file contains artifact metadata; validate the executed module output "
            "or use the integration test harness."
        )
        return 3

    errors = compare_dicts(generated, load_golden(Path(args.golden)), Decimal(args.tol))
    if errors:
        print("\n".join(errors))
        return 1

    print("OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
