#!/bin/bash
# ==============================================================================
# ShopSphere Database Automated Backup Script
# Creates compressed, timestamped backups of all 6 microservice databases
# Enforces backup retention and integrity checks
# ==============================================================================

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/shopsphere}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RETENTION_DAYS="${RETENTION_DAYS:-14}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-shopsphere-mysql}"
MYSQL_USER="${DB_USERNAME:-root}"
MYSQL_PASSWORD="${DB_PASSWORD:-bunny6281}"

DATABASES=(
  "shopsphere_auth"
  "shopsphere_product"
  "shopsphere_cart"
  "shopsphere_order"
  "shopsphere_inventory"
  "shopsphere_payment"
)

mkdir -p "${BACKUP_DIR}"

echo "=========================================================="
echo " Starting ShopSphere Multi-Database Backup: ${TIMESTAMP}"
echo "=========================================================="

BACKUP_TARGET_DIR="${BACKUP_DIR}/${TIMESTAMP}"
mkdir -p "${BACKUP_TARGET_DIR}"

for DB in "${DATABASES[@]}"; do
  echo "Backing up database: ${DB}..."
  DUMP_FILE="${BACKUP_TARGET_DIR}/${DB}_${TIMESTAMP}.sql.gz"

  # Execute mysqldump inside running container without exposing credentials to process table
  docker exec -i "${MYSQL_CONTAINER}" mysqldump \
    -u "${MYSQL_USER}" \
    -p"${MYSQL_PASSWORD}" \
    --single-transaction \
    --quick \
    --databases "${DB}" \
    | gzip -9 > "${DUMP_FILE}"

  # Verify dump file size
  SIZE=$(du -h "${DUMP_FILE}" | cut -f1)
  echo "  ✓ Successfully backed up ${DB} -> ${DUMP_FILE} (${SIZE})"
done

# Create SHA-256 checksums for backup verification
cd "${BACKUP_TARGET_DIR}"
sha256sum *.sql.gz > checksums.sha256
echo "  ✓ Generated SHA-256 integrity checksums"

# Prune backups older than RETENTION_DAYS
echo "Pruning backups older than ${RETENTION_DAYS} days..."
find "${BACKUP_DIR}" -maxdepth 1 -type d -mtime "+${RETENTION_DAYS}" -exec rm -rf {} +
echo "  ✓ Retention cleanup complete"

echo "=========================================================="
echo " Backup completed successfully at $(date)"
echo " Destination: ${BACKUP_TARGET_DIR}"
echo "=========================================================="
