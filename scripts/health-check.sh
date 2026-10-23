#!/bin/bash
# ==============================================================================
# ShopSphere Production Health & Readiness Verification Script
# Validates container health statuses and public HTTP/HTTPS endpoints
# ==============================================================================

set -euo pipefail

DOMAIN="${DOMAIN_NAME:-localhost}"
SCHEME="http"
PORT=""

# If running locally without TLS
if [ "${DOMAIN}" = "localhost" ]; then
  BASE_URL="http://localhost"
else
  BASE_URL="https://${DOMAIN}"
fi

echo "=========================================================="
echo " Checking ShopSphere Production Stack Health"
echo " Target Endpoint: ${BASE_URL}"
echo "=========================================================="

FAILED=0

# 1. Check Container States via Docker Compose
echo "Checking Docker container statuses..."
UNHEALTHY_CONTAINERS=$(docker compose -f docker-compose.prod.yml ps --format "{{.Name}}: {{.Health}}" | grep -v "healthy" || true)

if [ -n "${UNHEALTHY_CONTAINERS}" ]; then
  echo "  ! Some containers are not healthy yet:"
  echo "${UNHEALTHY_CONTAINERS}"
  FAILED=1
else
  echo "  ✓ All core containers report healthy status"
fi

# 2. Check Nginx Ingress /healthz
echo "Checking Nginx Ingress health..."
if curl -fsSL -k "${BASE_URL}/healthz" > /dev/null 2>&1; then
  echo "  ✓ Ingress /healthz returned HTTP 200 OK"
else
  echo "  ✗ Ingress /healthz failed"
  FAILED=1
fi

# 3. Check Frontend Storefront Root
echo "Checking Frontend Storefront..."
if curl -fsSL -k "${BASE_URL}/" | grep -qi "doctype html"; then
  echo "  ✓ Frontend SPA root returned valid HTML"
else
  echo "  ✗ Frontend SPA failed to respond"
  FAILED=1
fi

# 4. Check API Gateway Perimeter via Ingress
echo "Checking API Gateway perimeter routing (/api/v1/products)..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -k "${BASE_URL}/api/v1/products")

if [ "${HTTP_STATUS}" = "200" ]; then
  echo "  ✓ Public Product Browsing through Gateway returned HTTP 200"
else
  echo "  ✗ API Gateway returned unexpected HTTP ${HTTP_STATUS}"
  FAILED=1
fi

# 5. Verify Internal Ports are NOT Exposed on Host
echo "Verifying perimeter security (internal ports hidden)..."
for PORT in 3306 8081 8082 8083 8084 8085 8086 8761 8888; do
  if nc -z -w 1 127.0.0.1 "${PORT}" 2>/dev/null; then
    echo "  ✗ SECURITY WARNING: Port ${PORT} is accessible on host loopback!"
    FAILED=1
  fi
done
echo "  ✓ Verified: Microservice and database ports are not published to host"

echo "=========================================================="
if [ ${FAILED} -eq 0 ]; then
  echo " Overall Status: ALL CHECKS PASSED (HEALTHY)"
  exit 0
else
  echo " Overall Status: ONE OR MORE CHECKS FAILED"
  exit 1
fi
