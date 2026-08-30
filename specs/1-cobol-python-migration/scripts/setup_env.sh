#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VENV_DIR="$ROOT_DIR/.venv"
REQ="$ROOT_DIR/requirements.txt"

echo "Creating virtualenv in $VENV_DIR"
python3 -m venv "$VENV_DIR"
source "$VENV_DIR/bin/activate"
python -m pip install --upgrade pip
if [[ -f "$REQ" ]]; then
  pip install -r "$REQ"
else
  echo "requirements.txt not found in $ROOT_DIR"
fi

echo "Virtualenv ready. Activate with: source $VENV_DIR/bin/activate"

