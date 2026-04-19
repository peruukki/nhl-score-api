#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

CHECK_SCRIPT="$SCRIPT_DIR/github-checks-status.sh"
FIXTURES_DIR="$SCRIPT_DIR/fixtures/check-runs"

test_case () {
  file=$1
  expected=$2

  result=$(cat "$FIXTURES_DIR/$file" | "$CHECK_SCRIPT" "test-commit-sha" || true)

  if [ "$result" != "$expected" ]; then
    echo "❌ $file → expected $expected, got $result"
    exit 1
  else
    echo "✅ $file → $result"
  fi
}

test_case success.json "All checks passed for commit test-commit-sha"
test_case failure.json "Some checks have failed for commit test-commit-sha"
test_case pending.json "Checks are still running for commit test-commit-sha"
test_case rerun.json "All checks passed for commit test-commit-sha"
test_case none.json "No checks have been run for commit test-commit-sha"
