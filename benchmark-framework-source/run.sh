#!/bin/bash
set -e

if [ ! -d "target/classes" ]; then
    echo "Error: target/classes not found. Please run ./build.sh first."
    exit 1
fi

java -cp target/classes com.benchmark.Main "$@"
