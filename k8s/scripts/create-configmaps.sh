#!/usr/bin/env bash
# Puts ref-data's Flyway SQL into ConfigMaps; the Flyway init container mounts them as files (step 7).
set -euo pipefail

# Go to patient-demo/ first, then use only RELATIVE paths.
# A relative path does not start with "/", so Git Bash never has to convert it:
# the script works the same with or without MSYS_NO_PATHCONV, and on Linux/macOS too.
cd "$(dirname "$0")/../.."

kubectl create configmap ref-data-migration \
  --namespace patient-demo \
  --from-file=../ref-data-service/src/main/resources/db/migration \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create configmap ref-data-seed \
  --namespace patient-demo \
  --from-file=../ref-data-service/db/seed \
  --dry-run=client -o yaml | kubectl apply -f -

  # Keycloak realm: same file compose uses, so there is a single source of truth
  kubectl create configmap keycloak-realm \
    --namespace patient-demo \
    --from-file=keycloak/patient-demo-realm.json \
    --dry-run=client -o yaml | kubectl apply -f -
