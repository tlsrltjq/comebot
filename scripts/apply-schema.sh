#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

CONTAINER_NAME="comebot-postgres"
SCHEMA_FILE="src/main/resources/schema.sql"

if [[ -f ".env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source ".env"
  set +a
fi

POSTGRES_DB="${POSTGRES_DB:-comebot}"
POSTGRES_USER="${POSTGRES_USER:-comebot}"

if [[ ! -f "$SCHEMA_FILE" ]]; then
  echo "schema.sql not found: $SCHEMA_FILE"
  exit 1
fi

if ! docker inspect -f "{{.State.Running}}" "$CONTAINER_NAME" >/dev/null 2>&1; then
  echo "PostgreSQL container is not running: $CONTAINER_NAME"
  echo "Run: docker compose up -d postgres"
  exit 1
fi

echo "Applying schema.sql to PostgreSQL container $CONTAINER_NAME..."
docker exec -i "$CONTAINER_NAME" psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" < "$SCHEMA_FILE"
echo "schema.sql applied."
