#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

verify_docs() {
  python3 -m unittest discover -s scripts -p 'test_*.py' -v
  python3 scripts/check_stack_docs.py
}

verify_backend() {
  ./gradlew clean build --console=plain
  python3 scripts/check_test_reports.py backend
}

verify_frontend() {
  node -e 'if (process.versions.node.split(".")[0] !== "24") { throw new Error("Node 24 is required; select front/.nvmrc") }'
  (
    cd front
    npm ci
    mkdir -p test-results
    rm -f test-results/vitest.json
    npm test -- --reporter=default --reporter=json --outputFile=test-results/vitest.json
    npm run type-check
    npm run build-only
  )
  python3 scripts/check_test_reports.py frontend
}

verify_postgres() {
  ./gradlew postgresTest --console=plain
  python3 scripts/check_test_reports.py postgres
}

case "${1:-all}" in
  docs) verify_docs ;;
  backend) verify_backend ;;
  frontend) verify_frontend ;;
  postgres) verify_postgres ;;
  all) verify_docs; verify_backend; verify_frontend; verify_postgres ;;
  *) printf 'Usage: bash scripts/verify.sh [all|docs|backend|frontend|postgres]\n' >&2; exit 2 ;;
esac
