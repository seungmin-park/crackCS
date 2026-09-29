---
name: verify-crackcs
description: Verify the CrackCS stack documentation contract and select the existing backend, frontend, or visible user-flow checks for a change.
---

# Verify CrackCS

Run from the repository root. Start with [feature map](../../../docs/engineering/agent-workflow.md) and [official version index](../../../docs/engineering/stack-docs.md). Version declarations own selected versions; the docs index owns reviewed official URLs.

## Doctor

```bash
pwd
git status --short
python3 --version
python3 scripts/check_stack_docs.py
```

For Java changes, select Java 21 explicitly; a shell's default Java can be 17. For front changes, use Node 24 and the committed `front/.nvmrc`; `node --version` alone does not prove the selected engine.

## Drive the stack-docs path

```bash
python3 -m unittest discover -s scripts -p 'test_check_stack_docs.py' -v
python3 scripts/check_stack_docs.py
```

Require nonempty discovery and exit 0. Tests copy actual manifests to temporary folders, change one version or host, assert a nonzero result, and clean up their folders. Do not edit the working checkout to demonstrate failure.

For code changes, add focused checks at the affected boundary. Existing project commands:

```bash
./gradlew test --console=plain
cd front
npm ci
npm test
npm run type-check
npm run build-only
```

PostgreSQL contract changes additionally need `./gradlew postgresTest --console=plain` with Docker. Read [local runbook](../../../docs/changes/2026-09-27-release-readiness/local-runbook.md) before any app E2E. Match the user path in [product spec](../../../docs/product/spec.md); distinguish mock-driven tests, real DB checks, and actual model calls.

## Visible user flow

Local E2E must use the current caller's cmux workspace. Before starting, check `CMUX_WORKSPACE_ID`, `CMUX_SURFACE_ID`, and `cmux identify --json`. Reuse that workspace's E2E pane or create one to the caller's right with `--focus false`; include explicit workspace/surface in commands. Run servers and test logs there, use the workspace browser for actual clicks and inputs, inspect assertions and exit codes, and leave the result pane visible. Do not treat a preview or mock as a real model flow.

## Evidence and cleanup

Record revision/worktree, exact command, exit status, test count, and remaining unverified behavior in the relevant `docs/changes/` verification file. For stack-docs checks, [current evidence](../../../docs/changes/2026-09-29-agent-engineering/verification.md). The checker and tests create no persistent application state; temporary test directories self-clean. Stop only processes started for the current verification; keep the cmux result pane.
