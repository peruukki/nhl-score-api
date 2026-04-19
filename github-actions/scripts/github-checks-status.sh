#!/usr/bin/env bash
set -euo pipefail

COMMIT_SHA="${1:-unknown}"

# Accept JSON via stdin
input=$(cat)

runs=$(echo "$input" | jq '
  .check_runs
  | map(select(.name | startswith("checks / ")))
  | group_by(.name)
  | map(max_by(.started_at))
')

count=$(echo "$runs" | jq 'length')
has_pending=$(echo "$runs" | jq 'any(.conclusion == null)')
all_success=$(echo "$runs" | jq 'all(.conclusion == "success")')

if [ "$count" -eq 0 ]; then
  echo "No checks have been run for commit $COMMIT_SHA"
  exit 1
fi

if [ "$has_pending" = "true" ]; then
  echo "Checks are still running for commit $COMMIT_SHA"
  exit 1
fi

if [ "$all_success" != "true" ]; then
  echo "Some checks have failed for commit $COMMIT_SHA"
  exit 1
fi

echo "All checks passed for commit $COMMIT_SHA"
