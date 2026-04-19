#!/bin/sh
# Maven Wrapper script
# Simplified version for POC

set -e

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd "$(dirname "$0")" && pwd)}"
MAVEN_WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

# Check for Java
if [ -z "$JAVA_HOME" ]; then
  JAVACMD="java"
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

if ! command -v "$JAVACMD" > /dev/null 2>&1; then
  echo "Error: JAVA_HOME is not set and 'java' is not in your PATH." >&2
  exit 1
fi

# Run the wrapper
exec "$JAVACMD" \
  -jar "$MAVEN_WRAPPER_JAR" \
  "$@"
