#!/usr/bin/env bash
# extractor_stub.sh - Minimal extractor stub for local testing
# Usage: extractor_stub.sh --cobol <cobol_file> --out <out_ir.json>
set -euo pipefail

while [[ $# -gt 0 ]]; do
  case "$1" in
    --cobol) COBOL_FILE="$2"; shift 2 ;;
    --out) OUT_FILE="$2"; shift 2 ;;
    --help|-h) echo "Usage: $0 --cobol path/to/prog.cob --out path/to/ir.json"; exit 0 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done

if [[ -z "${COBOL_FILE:-}" || -z "${OUT_FILE:-}" ]]; then
  echo "Missing required args" >&2
  echo "Usage: $0 --cobol path/to/prog.cob --out path/to/ir.json" >&2
  exit 2
fi

# For the stub: if an example IR exists next to the cobol file with same base name, copy it.
DIR=$(dirname "$COBOL_FILE")
BASE=$(basename "$COBOL_FILE" .cob)
EXAMPLE_IR="$DIR/${BASE}.ir.json"

if [[ -f "$EXAMPLE_IR" ]]; then
  cp "$EXAMPLE_IR" "$OUT_FILE"
  echo "Wrote IR from example $EXAMPLE_IR -> $OUT_FILE"
  exit 0
fi

# Otherwise, fall back to copying a known example IR shipped with the spec
DEFAULT_IR="$(dirname "${BASH_SOURCE[0]}")/../examples/p1/ir_prog1.json"
if [[ -f "$DEFAULT_IR" ]]; then
  cp "$DEFAULT_IR" "$OUT_FILE"
  echo "Wrote default IR -> $OUT_FILE"
  exit 0
fi

# If nothing available, create a minimal empty IR
cat > "$OUT_FILE" <<EOF
{ "programs": [] }
EOF

echo "Wrote empty IR to $OUT_FILE"
exit 0

