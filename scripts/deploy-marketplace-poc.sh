#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EXAMPLE_DIR="$ROOT_DIR/examples/legacy-modernization-poc"

ENVIRONMENT="${ENVIRONMENT:-dev}"
AWS_REGION="${AWS_REGION:-us-east-1}"
STACK_NAME="${STACK_NAME:-modernash-legacy-poc-$ENVIRONMENT}"

if [[ ! -d "$EXAMPLE_DIR" ]]; then
  echo "Example directory not found: $EXAMPLE_DIR" >&2
  exit 1
fi

REQUIRED_CMDS=(python3 sam aws)
for cmd in "${REQUIRED_CMDS[@]}"; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing required command: $cmd" >&2
    exit 1
  fi
done

echo "== Running PoC tests =="
python3 -m unittest discover -s "$EXAMPLE_DIR/tests" -p "test_*.py"

echo "== Building SAM application =="
sam build \
  --template-file "$EXAMPLE_DIR/template.yaml" \
  --build-dir "$EXAMPLE_DIR/.aws-sam/build"

echo "== Deploying stack =="
SAM_DEPLOY_ARGS=(
  --template-file "$EXAMPLE_DIR/.aws-sam/build/template.yaml"
  --stack-name "$STACK_NAME"
  --region "$AWS_REGION"
  --capabilities CAPABILITY_IAM
  --resolve-s3
  --no-confirm-changeset
  --no-fail-on-empty-changeset
  --parameter-overrides "Environment=$ENVIRONMENT"
)

if [[ -n "${AWS_PROFILE:-}" ]]; then
  SAM_DEPLOY_ARGS+=(--profile "$AWS_PROFILE")
fi

sam deploy "${SAM_DEPLOY_ARGS[@]}"

echo "Deployment complete."
echo "Stack name: $STACK_NAME"
echo "Region: $AWS_REGION"
