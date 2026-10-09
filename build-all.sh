#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
./gradlew buildAllToOut --no-parallel -PuseLocalVolmLib=false "$@"
