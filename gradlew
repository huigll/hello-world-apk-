#!/usr/bin/env sh

# Minimal Gradle wrapper script
# Generated-style wrapper script (simplified) to run ./gradlew on Linux.

DIR="$(cd "$(dirname "$0")" && pwd)"

JAVA_HOME=${JAVA_HOME:-""}
if [ -z "$JAVA_HOME" ]; then
  JAVA_CMD="java"
else
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

CLASSPATH="$DIR/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVA_CMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
