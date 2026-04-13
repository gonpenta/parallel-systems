#!/bin/bash
set -e

echo "Compiling benchmark framework..."
mkdir -p target/classes
javac -d target/classes $(find src -name "*.java")
echo "Build successful. Output in target/classes"
