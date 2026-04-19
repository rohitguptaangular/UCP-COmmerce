#!/bin/bash
# SAP Commerce Cloud UCP POC — Setup & Run Script
# This script installs Java 17 + Maven and runs the application.

set -e

echo "=== SAP Commerce Cloud UCP POC ==="
echo ""

# Check if Java 17+ is available
JAVA_VERSION=$(java -version 2>&1 | head -1 | awk -F '"' '{print $2}' | cut -d. -f1)
if [ -z "$JAVA_VERSION" ] || [ "$JAVA_VERSION" -lt 17 ] 2>/dev/null; then
    echo "Java 17+ required. Install options:"
    echo ""
    echo "  Option 1 (macOS with Homebrew):"
    echo "    brew install openjdk@17"
    echo "    export JAVA_HOME=\$(brew --prefix openjdk@17)"
    echo "    export PATH=\$JAVA_HOME/bin:\$PATH"
    echo ""
    echo "  Option 2 (SDKMAN - any OS):"
    echo "    curl -s https://get.sdkman.io | bash"
    echo "    source ~/.sdkman/bin/sdkman-init.sh"
    echo "    sdk install java 17.0.11-tem"
    echo ""
    echo "  Option 3 (Docker):"
    echo "    docker run --rm -v \$(pwd):/app -w /app -p 8080:8080 eclipse-temurin:17 ./mvnw spring-boot:run"
    echo ""
    exit 1
fi

echo "✓ Java version: $JAVA_VERSION"

# Check if Maven is available (use wrapper or system maven)
if [ -f "./mvnw" ] && [ -x "./mvnw" ]; then
    MVN="./mvnw"
elif command -v mvn > /dev/null 2>&1; then
    MVN="mvn"
else
    echo "Maven not found. Installing Maven wrapper..."
    echo "Or install with: brew install maven"
    exit 1
fi

echo "✓ Using Maven: $MVN"
echo ""
echo "=== Building project ==="
$MVN clean package -DskipTests -q

echo ""
echo "=== Starting SAP UCP POC on port 8080 ==="
echo ""
echo "Test endpoints:"
echo "  curl http://localhost:8080/.well-known/ucp"
echo "  curl -X POST http://localhost:8080/ucp/api/checkout/sessions"
echo "  curl http://localhost:8080/ucp/api/checkout/products"
echo ""
$MVN spring-boot:run
