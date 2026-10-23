#!/bin/bash
# ==============================================================================
# ShopSphere Let's Encrypt Automated Certificate Provisioning Script
# Initializes valid production TLS/HTTPS certificates using Certbot ACME webroot
# ==============================================================================

set -euo pipefail

DOMAIN="${DOMAIN_NAME:-}"
EMAIL="${SSL_EMAIL:-}"
STAGING="${CERTBOT_STAGING:-0}" # Set to 1 for staging testing against Let's Encrypt limits

if [ -z "${DOMAIN}" ] || [ "${DOMAIN}" = "localhost" ]; then
  echo "Error: DOMAIN_NAME environment variable is required (and cannot be localhost for public TLS)!"
  echo "Example: export DOMAIN_NAME=shopsphere.yourdomain.com"
  exit 1
fi

if [ -z "${EMAIL}" ]; then
  echo "Error: SSL_EMAIL environment variable is required for certificate expiry notices!"
  echo "Example: export SSL_EMAIL=admin@yourdomain.com"
  exit 1
fi

DATA_PATH="./data/certbot"
mkdir -p "${DATA_PATH}/conf/live/${DOMAIN}"
mkdir -p "${DATA_PATH}/www"

echo "=========================================================="
echo " Initializing TLS Certificates for: ${DOMAIN}"
echo " Contact Email: ${EMAIL}"
echo "=========================================================="

# 1. Create dummy certificate if fullchain does not exist yet (allows Nginx to boot)
if [ ! -f "${DATA_PATH}/conf/live/${DOMAIN}/fullchain.pem" ]; then
  echo "Creating temporary self-signed certificate for initial Nginx boot..."
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout "${DATA_PATH}/conf/live/${DOMAIN}/privkey.pem" \
    -out "${DATA_PATH}/conf/live/${DOMAIN}/fullchain.pem" \
    -subj "/CN=localhost"
  echo "  ✓ Temporary certificate created"
fi

# 2. Start Nginx reverse proxy
echo "Starting Nginx reverse proxy to handle ACME challenge..."
docker compose -f docker-compose.prod.yml up -d nginx-proxy
sleep 5

# 3. Request genuine Let's Encrypt certificate
echo "Requesting genuine Let's Encrypt SSL certificate..."
STAGING_ARG=""
if [ "${STAGING}" != "0" ]; then
  STAGING_ARG="--staging"
fi

docker run --rm \
  -v "$(pwd)/${DATA_PATH}/conf:/etc/letsencrypt" \
  -v "$(pwd)/${DATA_PATH}/www:/var/www/certbot" \
  certbot/certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  ${STAGING_ARG} \
  --email "${EMAIL}" \
  --agree-tos \
  --no-eff-email \
  --force-renewal \
  -d "${DOMAIN}"

# 4. Reload Nginx with genuine certificate
echo "Reloading Nginx with production TLS certificate..."
docker compose -f docker-compose.prod.yml exec nginx-proxy nginx -s reload

echo "=========================================================="
echo " HTTPS Setup Completed Successfully!"
echo " Domain: https://${DOMAIN}/"
echo "=========================================================="
