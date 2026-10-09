#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
sources_file="$(mktemp)"
trap 'rm -f "$sources_file"' EXIT
find src -name '*.java' -print | sort > "$sources_file"
rm -rf out
mkdir -p out
javac --release 25 --add-modules jdk.incubator.vector -d out "@$sources_file"
jar --create --file simd-bench.jar --main-class simdbench.Bench -C out .
