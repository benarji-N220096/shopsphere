#!/bin/bash
# ==============================================================================
# ShopSphere Database Restoration Script
# Restores all 6 microservice databases from an existing compressed backup
# Verifies SHA-256 checksums before applying to prevent data corruption
# ==============================================================================

set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: $0 <path-to-backup-directory> [--force]"
  echo "Example: $0 /var/backups/shopsphere/20261023_120000"
  exit 1
fi

BACKUP_TARGET_DIR="$1"
FORCE_FLAG="${2:-}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-shopsphere-mysql}"
MYSQL_USER="${DB_USERNAME:-root}"
MYSQL_PASSWORD="${DB_PASSWORD:-bunny6281}"

if [ ! -d "${BACKUP_TARGET_DIR}" ]; then
  echo "Error: Directory ${BACKUP_TARGET_DIR} does not exist!"
  exit 1
fi

echo "=========================================================="
echo " ShopSphere Multi-Database Restoration"
echo " Target Backup: ${BACKUP_TARGET_DIR}"
echo "=========================================================="

# 1. Verify Checksums
if [ -f "${BACKUP_TARGET_DIR}/checksums.sha256" ]; then
  echo "Verifying backup SHA-256 checksums..."
  (cd "${BACKUP_TARGET_DIR}" && sha256sum -c checksums.sha256)
  echo "  ✓ Checksums verified successfully"
else
  echo "Warning: checksums.sha256 not found. Proceeding with caution..."
fi

# 2. Safety Confirmation
if [ "${FORCE_FLAG}" != "--force" ]; then
  echo ""
  echo "WARNING: This operation will overwrite existing database records in MySQL!"
  read -r -p "Are you sure you want to proceed with restoration? (yes/no): " CONFIRM
  if [ "${CONFIRM}" != "yes" ]; then
    echo "Restoration aborted by user."
    exit 0
  fi
fi

DATABASES=(
  "shopsphere_auth"
  "shopsphere_product"
  "shopsphere_cart"
  "shopsphere_order"
  "shopsphere_inventory"
  "shopsphere_payment"
)

# 3. Restore each schema
for DB in "${DATABASES[@]}"; do
  DUMP_FILE=$(ls "${BACKUP_TARGET_DIR}/${DB}_"*.sql.gz 2>/dev/null | head -n 1)
  if [ -n "${DUMP_FILE}" ] && [ -f "${DUMP_FILE}" ]; then
    echo "Restoring ${DB} from ${DUMP_FILE}..."
    gunzip -c "${DUMP_FILE}" | docker exec -i "${MYSQL_CONTAINER}" mysql \
      -u "${MYSQL_USER}" \
      -p"${MYSQL_PASSWORD}" \
      "${DB}"
    echo "  ✓ Successfully restored ${DB}"
  else
    echo "  ! Skipping ${DB} (no matching dump file found)"
  fi
done

echo "=========================================================="
echo " Restoration completed successfully at $(date)"
echo "=========================================================="
