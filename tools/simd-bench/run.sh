#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ ! -f simd-bench.jar ]] || find src -name '*.java' -newer simd-bench.jar | grep -q .; then
    ./build.sh
fi
java --add-modules jdk.incubator.vector -jar simd-bench.jar "$@"
