#!/usr/bin/env bash
# Builds both service images and loads them into the kind cluster.
set -euo pipefail

cd "$(dirname "$0")/../.."          # patient-demo/, then relative paths only

docker build -t patient-service:local .
docker build -t ref-data-service:local ../ref-data-service
kind load docker-image patient-service:local ref-data-service:local --name patient-demo
