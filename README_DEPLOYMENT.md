# ShopSphere — Production Deployment & Containerization Guide
**Phase 11 — Part 1: Docker Containerization & Local Production Stack**

This guide provides end-to-end instructions for running, building, verifying, and troubleshooting the ShopSphere microservices architecture using Docker and Docker Compose.

---

## 1. Prerequisites & Required Tools

To run ShopSphere in a containerized environment, ensure the following are installed:

- **Docker Desktop**: Version 24.0+ (or Docker Engine 24.0+)
- **Docker Compose**: Version v2.20+
- **Git**: For version control
- **System Memory**: Minimum 8 GB RAM (16 GB recommended for running all 10 containers + observability concurrently)
- **Disk Space**: At least 10 GB free space for Docker images and build caches

*For running locally without Docker (Host Mode):*
- **Java Development Kit (JDK)**: OpenJDK 21 (Eclipse Temurin 21 recommended)
- **Node.js & npm**: Node 20.x or 22.x LTS
- **MySQL Server**: MySQL 8.0 on port 3306
- **Python**: 3.10+ (for verification test runners)

---

## 2. Environment Setup & Configuration

1. **Clone the repository**:
   ```bash
   git clone <repo-url> shosphere
   cd shosphere
   ```

2. **Configure Environment Variables**:
   Copy the `.env.example` template to `.env`:
   ```bash
   cp .env.example .env
   ```
   Inspect and customize values as necessary. The default values in `.env.example` match the local development defaults:
   - `DB_PASSWORD=bunny6281`
   - `JWT_SECRET=8f2a1b9c8e4d3f2a1b9c8e4d3f2a1b9c8e4d3f2a1b9c8e4d3f2a1b9c8e4d3f2a`
   - `INTERNAL_SERVICE_SECRET=shopsphere-internal-service-secret-key-384`

---

## 3. Quickstart: Running with Docker Compose

### Step 1: Build Container Images
Build all 10 application images (multi-stage builds for JDK 21 and Node 22/Nginx):
```bash
docker compose build
```

To build a specific service:
```bash
docker compose build auth-service
docker compose build frontend
```

### Step 2: Start the Container Stack
Start all services in detached mode:
```bash
docker compose up -d
```

### Step 3: Check Container Startup and Health Status
```bash
docker compose ps
```
All services define robust health checks (`service_healthy` condition) ensuring proper startup ordering:
1. `mysql` (starts and initializes all 6 schemas via `/docker-entrypoint-initdb.d/01-init-databases.sql`)
2. `config-server` (loads configs from `/app/config-repository`)
3. `service-registry` (Eureka server on port 8761)
4. Core Microservices (`auth-service`, `product-service`, `cart-service`, `order-service`, `inventory-service`, `payment-service`)
5. `api-gateway` (perimeter gateway and JWT validation)
6. `frontend` (production Nginx server serving built React SPA)
7. Observability (`prometheus`, `grafana`, `zipkin`)

---

## 4. Service Endpoints & Access URLs

| Application / Tier | Host URL | Description | Credentials / Details |
| :--- | :--- | :--- | :--- |
| **Frontend Storefront** | [http://localhost:5173/](http://localhost:5173/) | React Storefront SPA | Customer & Admin Web UI |
| **API Gateway** | [http://localhost:8080/](http://localhost:8080/) | Public API Perimeter | Strips internal headers; routes `/api/v1/**` |
| **Eureka Service Registry**| [http://localhost:8761/](http://localhost:8761/) | Service Discovery Dashboard | Inspect registered microservices |
| **Config Server** | [http://localhost:8888/](http://localhost:8888/) | Spring Cloud Config | Native search location: `file:/app/config-repository/` |
| **Prometheus** | [http://localhost:9090/](http://localhost:9090/) | Metrics Scraper & TSDB | Scrapes `/actuator/prometheus` across services |
| **Grafana** | [http://localhost:3000/](http://localhost:3000/) | Observability Dashboards | User: `admin` / Password: `admin` |
| **Zipkin** | [http://localhost:9411/](http://localhost:9411/) | Distributed Tracing UI | Micrometer Brave span tracing |
| **MySQL Server** | `localhost:3306` | Multi-Schema Database | User: `root` / Password: `${DB_PASSWORD}` |

---

## 5. Health Verification Commands

Verify health of running containers via Docker health status:
```bash
# View health status of all containers
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

# Inspect health check logs for a specific service
docker inspect --format='{{json .State.Health}}' shopsphere-api-gateway
docker inspect --format='{{json .State.Health}}' shopsphere-auth-service
```

Direct HTTP health probes:
```bash
# API Gateway Health
curl -s http://localhost:8080/actuator/health

# Config Server Health
curl -s http://localhost:8888/actuator/health

# Eureka Registry Health
curl -s http://localhost:8761/actuator/health

# Frontend Availability
curl -I http://localhost:5173/
```

---

## 6. Shutdown & Maintenance

### Normal Graceful Shutdown:
```bash
docker compose down
```

### Shutdown Preserving Data:
```bash
# Stops and removes containers and networks; PERSISTS all database data in shopsphere_mysql_data volume
docker compose down
```

### CAUTION: Removing Persistent Data:
> [!CAUTION]
> Running `docker compose down -v` permanently deletes the persistent MySQL volume (`shopsphere_mysql_data`). Never run this command in production or without an explicit backup!

---

## 7. Running Locally Without Docker (Host Mode)

If you need to run the application on your host machine without Docker:

1. **Start MySQL**:
   Ensure MySQL 8.0 is running locally on port 3306 with password `bunny6281`.
   Ensure schemas exist: `shopsphere_auth`, `shopsphere_product`, `shopsphere_cart`, `shopsphere_order`, `shopsphere_inventory`, `shopsphere_payment`.

2. **Automated Startup Script**:
   Execute the automated PowerShell startup script:
   ```powershell
   .\start-shopsphere.ps1
   ```
   This script:
   - Starts Config Server (`:8888`) and Eureka (`:8761`)
   - Waits for health readiness
   - Starts all 6 backend microservices
   - Starts API Gateway (`:8080`) and Frontend Vite Dev server (`:5173`)

3. **Running Individual Services**:
   ```bash
   cd services/auth-service
   ./mvnw spring-boot:run
   ```

---

## 8. Troubleshooting Guidance

### 1. Port Conflicts (e.g. `Bind for 0.0.0.0:8080 failed: port is already allocated`):
- Check for existing processes running on host ports:
  ```powershell
  Get-NetTCPConnection -LocalPort 8080, 8081, 8082, 8083, 8084, 8085, 8086, 8761, 8888, 5173 -ErrorAction SilentlyContinue
  ```
- Terminate lingering Java or Node processes before running `docker compose up`.

### 2. Microservice Fails to Register with Eureka:
- Check `docker compose logs service-registry` to verify Eureka is ready.
- Check service logs: `docker compose logs <service-name>`.
- Verify `EUREKA_SERVER_URL` in `.env` is set to `http://service-registry:8761/eureka/`.

### 3. Flyway Migration Failures:
- Check `docker compose logs <service-name>`.
- Verify that `mysql` initialized all 6 schemas via `/docker-entrypoint-initdb.d/01-init-databases.sql`.
- In clean environments, schemas are auto-created by the init script on first database volume boot.

### 4. Frontend Cannot Connect to Backend:
- In production containers, Nginx proxies `/api/` to `http://api-gateway:8080/api/` inside the Docker network.
- Ensure `shopsphere-api-gateway` container is healthy:
  ```bash
  docker compose ps api-gateway
  ```

---

## 9. Current Status & Cloud Readiness (Phase 11 Part 2)

- **Containerization**: Complete. Multi-stage Dockerfiles for all 10 applications with Eclipse Temurin JDK 21 Alpine and Node 22/Nginx 1.27 Alpine.
- **Root Docker Compose**: Fully configured with container dependencies, health checks, bridge network, named volume persistence, and environment variable bindings.
- **Security Boundaries**: Intact. `payment-service` strictly internal with no host port published. Perimeter header stripping active. JWT HS384 authentication preserved.
- **Phase 11 Part 2 Scope**: Deployment to Cloud Kubernetes (EKS / GKE / AKS) or Cloud ECS, public domain DNS, TLS termination, and ingress controllers will be performed in Part 2.
