#!/bin/bash
set -e

BACKUP_FILE=$1
if [ -z "$BACKUP_FILE" ]; then
  echo "Usage: ./verify-backup.sh <path_to_backup.sql>"
  exit 1
fi

TEMP_DB_NAME="restore_verify_$(date +%s)"
echo "Starting verification for $BACKUP_FILE using temp DB $TEMP_DB_NAME..."

# 1. Create temporary DB in the running postgres container
docker compose -f docker-compose.prod.yml exec -t postgres psql -U "${POSTGRES_USER:-letapay}" -c "CREATE DATABASE $TEMP_DB_NAME;"

# 2. Restore backup to temp DB
cat "$BACKUP_FILE" | docker compose -f docker-compose.prod.yml exec -T postgres psql -U "${POSTGRES_USER:-letapay}" -d "$TEMP_DB_NAME"

# 3. Verify schema/data (e.g. check if 'sessions' table exists and can be queried)
echo "Verifying 'sessions' table..."
docker compose -f docker-compose.prod.yml exec -t postgres psql -U "${POSTGRES_USER:-letapay}" -d "$TEMP_DB_NAME" -c "SELECT count(*) FROM sessions;"

# 4. Cleanup
docker compose -f docker-compose.prod.yml exec -t postgres psql -U "${POSTGRES_USER:-letapay}" -c "DROP DATABASE $TEMP_DB_NAME;"

echo "✅ Backup verification successful for $BACKUP_FILE"
