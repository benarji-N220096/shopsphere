# ==============================================================================
# ShopSphere Storefront - Multi-Stage Production Dockerfile
# Stage 1: Build with Node 22 on Alpine
# Stage 2: Serve high-performance static assets via Nginx 1.27 on Alpine
# ==============================================================================

# Stage 1: Frontend Build Stage
FROM node:22-alpine AS builder
WORKDIR /app

# Copy dependency manifests
COPY package.json package-lock.json* ./

# Install clean dependencies
RUN npm ci || npm install

# Copy source assets
COPY . .

# Build production distribution bundle
RUN npm run build

# Stage 2: Nginx Web Server Runtime Stage
FROM nginx:1.27-alpine
LABEL maintainer="ShopSphere DevOps Team"
LABEL description="ShopSphere Production Frontend Web Server"

# Remove default nginx static page
RUN rm -rf /usr/share/nginx/html/*

# Copy production build artifacts from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx SPA and reverse proxy configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --retries=3 --start-period=5s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
