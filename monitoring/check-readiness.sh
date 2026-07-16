#!/bin/sh
set -eu

url="${1:-http://localhost:8080/actuator/health/readiness}"
timeout="${SYNTHETIC_TIMEOUT_SECONDS:-5}"

curl --fail --silent --show-error --max-time "$timeout" "$url" >/dev/null
