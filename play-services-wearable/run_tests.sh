#!/bin/bash
# Wear OS test runner — comprehensive test suite execution
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

echo "=========================================="
echo "  microG Wear OS Test Suite"
echo "=========================================="
echo ""

# Unit tests
echo "[1/3] Running unit tests..."
if ./gradlew :play-services-wearable:core:test --no-daemon 2>&1 | tail -5; then
    echo -e "${GREEN}✓ Unit tests passed${NC}"
else
    echo -e "${RED}✗ Unit tests failed${NC}"
    exit 1
fi

# Lint
echo ""
echo "[2/3] Running lint checks..."
if ./gradlew :play-services-wearable:core:lint --no-daemon 2>&1 | tail -5; then
    echo -e "${GREEN}✓ Lint passed${NC}"
else
    echo -e "${RED}✗ Lint failed${NC}"
fi

# Coverage report
echo ""
echo "[3/3] Generating coverage report..."
./gradlew :play-services-wearable:core:testDebugUnitTestCoverage --no-daemon 2>&1 | tail -3

echo ""
echo "=========================================="
echo "  Test suite complete"
echo "=========================================="
