#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

SERVICES="
config-service
discovery-service
gateway-service
auth-service
customer-service
account-service
transaction-service
loan-service
"

for service in $SERVICES; do
  echo "==> Building $service"
  (
    cd "$ROOT_DIR/microservices-backend/$service"
    ./mvnw clean package -Dmaven.test.skip=true
  )
done

echo "==> Java services built successfully."
