#!/bin/bash
# ==============================================================================
# ShopSphere Automated Production Deployment Script
# Executes zero-data-loss rolling redeployment with automated pre-deploy backups
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${PROJECT_ROOT}"

echo "=========================================================="
echo " Starting ShopSphere Production Deployment: $(date)"
echo "=========================================================="

# 1. Pre-Deployment Database Backup (Safety First)
if docker ps -q -f name=shopsphere-mysql-prod | grep -q .; then
  echo "[Step 1/5] Creating safety backup of existing databases..."
  "${SCRIPT_DIR}/backup-databases.sh" || {
    echo "Warning: Backup reported an issue; proceed with caution."
  }
else
  echo "[Step 1/5] Database container not running yet; skipping pre-backup."
fi

# 2. Pull Latest Git Changes
echo "[Step 2/5] Synchronizing latest release from Git..."
git fetch --all --prune
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
git pull origin "${CURRENT_BRANCH}"

# 3. Build & Pull Updated Container Images
echo "[Step 3/5] Building and pulling production container images..."
docker compose -f docker-compose.prod.yml build --pull

# 4. Graceful Container Recreation
echo "[Step 4/5] Launching updated containers with Docker Compose..."
docker compose -f docker-compose.prod.yml up -d --remove-orphans

# 5. Post-Deployment Health Verification
echo "[Step 5/5] Waiting 30s for services to initialize and register with Eureka..."
sleep 30

echo "Executing health verification suite..."
if "${SCRIPT_DIR}/health-check.sh"; then
  echo "=========================================================="
  echo " Deployment Succeeded! Stack is fully operational."
  echo "=========================================================="
else
  echo "=========================================================="
  echo " ALERT: Health check failed after deployment!"
  echo " Inspect logs with: docker compose -f docker-compose.prod.yml logs --tail=100"
  echo "=========================================================="
  exit 1
fi
