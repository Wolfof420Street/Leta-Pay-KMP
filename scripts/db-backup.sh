#!/bin/bash
set -e

# Configuration
BACKUP_DIR="${1:-./backups}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/letapay_db_${TIMESTAMP}.sql"
DB_USER="${POSTGRES_USER:-letapay}"
DB_NAME="${POSTGRES_DB:-letapay}"

mkdir -p "$BACKUP_DIR"

echo "Starting database backup to $BACKUP_FILE..."

# Run pg_dump in the container
docker compose -f docker-compose.prod.yml exec -t postgres pg_dump -U "$DB_USER" "$DB_NAME" > "$BACKUP_FILE"

echo "Backup completed: $BACKUP_FILE"

# Retention: Keep last 7 days
find "$BACKUP_DIR" -name "letapay_db_*.sql" -mtime +7 -delete

echo "Old backups cleaned up."
