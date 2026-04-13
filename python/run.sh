#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
pip install -r requirements.txt -q
export PYTHONPATH="$PWD"
exec python -m benchmark "$@"
