#!/usr/bin/env bash
# Definition-of-done gate for every task (AGENTS.md, plan §0): build, unit tests, lint.
# Exits non-zero if any of them fails.
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug "$@"
