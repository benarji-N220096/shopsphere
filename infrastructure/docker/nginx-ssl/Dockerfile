# ==============================================================================
# ShopSphere Production Nginx Reverse Proxy Dockerfile
# Serves as the single perimeter ingress with TLS termination and rate limiting
# ==============================================================================

FROM nginx:1.27-alpine
LABEL maintainer="ShopSphere DevOps Team"
LABEL description="ShopSphere Production Edge Reverse Proxy with TLS"

# Remove default configurations
RUN rm -rf /etc/nginx/conf.d/* /etc/nginx/nginx.conf

# Create directories for certbot ACME webroot and SSL certificates
RUN mkdir -p /var/www/certbot /etc/letsencrypt

# Copy hardened nginx configuration template
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80 443

HEALTHCHECK --interval=15s --timeout=3s --retries=3 --start-period=5s \
  CMD wget --no-verbose --tries=1 --spider http://localhost/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
