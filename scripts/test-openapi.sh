#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
SPEC_PATH="${SPEC_PATH:-openapi.yaml}"

if command -v schemathesis >/dev/null 2>&1; then
  SCHEMATHESIS_CMD="schemathesis"
else
  if command -v python >/dev/null 2>&1; then
    if python -c "import schemathesis" >/dev/null 2>&1; then
      SCHEMATHESIS_CMD="python -m schemathesis.cli"
    else
      echo "schemathesis is not installed in this Python environment."
      echo "Install with: python -m pip install schemathesis"
      exit 1
    fi
  else
    echo "schemathesis is not installed. Install with: pip install schemathesis"
    exit 1
  fi
fi

if [ ! -f "$SPEC_PATH" ]; then
  echo "OpenAPI spec not found at $SPEC_PATH"
  echo "Generate it with: curl -s $BASE_URL/v3/api-docs.yaml -o $SPEC_PATH"
  exit 1
fi

${SCHEMATHESIS_CMD} run "$SPEC_PATH" --url "$BASE_URL"
