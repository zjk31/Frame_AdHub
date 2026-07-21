#!/usr/bin/env bash
set -e

echo "============================================"
echo " Harness Performance & Lint Check"
echo "============================================"
echo ""

echo "[1/2] Running Lint..."
./gradlew lint
echo "Lint: PASSED"
echo ""

echo "[2/2] Running Unit Tests..."
./gradlew test
echo "Tests: PASSED"
echo ""

echo "============================================"
echo " All checks passed. Ready to commit."
echo "============================================"
