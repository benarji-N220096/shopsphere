# ShopSphere — Production Architecture Specification
## Phase 11 Part 2: Cloud Production Architecture & Boundary Definition

---

## 1. High-Level Architectural Diagram

```
                                      [ Internet / User Browser ]
                                                   |
                             +---------------------+---------------------+
                             |                                           |
                             v HTTP (Port 80)                            v HTTPS (Port 443)
                  +-----------------------------------------------------------------+
                  |          Production Nginx Edge Reverse Proxy (Port 80/443)      |
                  |                                                                 |
                  |  - Automated 301 Redirect: HTTP -> HTTPS                        |
                  |  - Let's Encrypt TLS Termination (Certbot Companion)            |
                  |  - Hardened Headers: HSTS, CSP, X-Frame-Options, X-Content-Type |
                  |  - Rate Limiting: 10 req/sec per IP, burst 20                   |
                  |  - Public Ingress Health Check: /healthz                        |
                  +--------------------------------+--------------------------------+
                                                   |
               +-----------------------------------+-----------------------------------+
               |                                                                       |
               v (Proxy / )                                                            v (Proxy /api/ )
+-------------------------------+                             +-------------------------------------------------+
|   frontend (React 19 / SPA)   |                             |             api-gateway (Spring Cloud)          |
|                               |                             |                                                 |
| - Served via Nginx 1.27 Alpine|                             | - Strips X-User-Id & X-Internal-* at perimeter  |
| - SPA Routing (try_files)     |                             | - Validates HS384 JWT Tokens on protected paths |
| - Static Asset Caching        |                             | - Routes: /api/v1/auth, products, cart, orders  |
| - No Secrets Embedded         |                             | - Blocks: /orders/*/recover, /inventory/reserve |
+-------------------------------+                             +------------------------+------------------------+
                                                                                       |
      +--------------------------------------------------------------------------------+
      | (Internal Service-to-Service Communication via Eureka Discovery & Docker DNS: shopsphere_prod_network)
      |
      +-------------------+-------------------+-------------------+-------------------+-------------------+
      |                   |                   |                   |                   |                   |
      v                   v                   v                   v                   v                   v
+-------------+     +-------------+     +-------------+     +-------------+     +-------------+     +-------------+
| auth-service|     |product-serv |     | cart-service|     |order-service|     |inventory-srv|     |payment-serv |
|   (:8081)   |     |   (:8082)   |     |   (:8083)   |     |   (:8084)   |     |   (:8085)   |     |   (:8086)   |
|             |     |             |     |             |     |             |     |             |     | [INTERNAL]  |
| - User Auth |     | - Catalog   |     | - User Carts|     | - SAGA Orch |     | - Stock     |     | - Process   |
| - JWT Mint  |     | - Category  |     | - Ownership |     | - Recovery  |     | - Reserve   |     |   Charge    |
+------+------+     +------+------+     +------+------+     +------+------+     +------+------+     +------+------+
       |                   |                   |                   |                   |                   |
       |                   |                   |                   | (Feign Client)    |                   |
       |                   |                   |                   +-------------------+------------------>|
       |                   |                   |                   |                   |                   |
       |                   |                   |                   | (Feign Client)    |                   |
       |                   |                   |                   +------------------>|                   |
       |                   |                   |                   |                                       |
+------+-------------------+-------------------+-------------------+---------------------------------------+------+
|                                                 DATABASE LAYER                                                  |
|                                                                                                                 |
|                                         [ MySQL 8.0 Engine (:3306) ]                                            |
|                                 (Named Volume: shopsphere_mysql_prod_data)                                      |
|                                                                                                                 |
|  +--------------------+  +--------------------+  +--------------------+  +-----------------------------------+  |
|  |  shopsphere_auth   |  | shopsphere_product |  |  shopsphere_cart   |  |        shopsphere_order           |  |
|  |  Owner: auth_user  |  | Owner: product_user|  |  Owner: cart_user  |  |        Owner: order_user          |  |
|  +--------------------+  +--------------------+  +--------------------+  +-----------------------------------+  |
|  +--------------------------------------------+  +-----------------------------------------------------------+  |
|  |           shopsphere_inventory             |  |                    shopsphere_payment                     |  |
|  |           Owner: inventory_user            |  |                    Owner: payment_user                    |  |
|  +--------------------------------------------+  +-----------------------------------------------------------+  |
+-----------------------------------------------------------------------------------------------------------------+

====================================== [ CLUSTER INFRASTRUCTURE & OBSERVABILITY ] =====================================
  +-------------------------------+    +-------------------------------+    +-------------------------------+
  |     config-server (:8888)     |    |    service-registry (:8761)   |    |         zipkin (:9411)        |
  |  Centralized Spring Cloud     |    |    Netflix Eureka Registry    |    |  Distributed Tracing & Spans  |
  +-------------------------------+    +-------------------------------+    +-------------------------------+
  +-------------------------------+    +-------------------------------+
  |       prometheus (:9090)      |    |        grafana (:3000)        |
  |   Metrics Scraper & Engine    |    |   Operational Metrics Boards  |
  +-------------------------------+    +-------------------------------+
```

---

## 2. Ingress & Perimeter Exposure Matrix

| Tier / Service | Container Port | External Port | Exposure Level | Protocol / Method |
| :--- | :--- | :--- | :--- | :--- |
| **Nginx Edge Proxy** | 80, 443 | **80, 443** | **Public Internet** | HTTP/1.1 (Redirect) & HTTP/2 (TLS 1.3) |
| **Frontend Storefront** | 80 | *None* | **Internal Only** | Proxied from Edge Nginx (`/`) |
| **API Gateway** | 8080 | *None* | **Internal Only** | Proxied from Edge Nginx (`/api/`) |
| **Auth Service** | 8081 | *None* | **Internal Only** | Routed through API Gateway |
| **Product Service** | 8082 | *None* | **Internal Only** | Routed through API Gateway |
| **Cart Service** | 8083 | *None* | **Internal Only** | Routed through API Gateway |
| **Order Service** | 8084 | *None* | **Internal Only** | Routed through API Gateway |
| **Inventory Service** | 8085 | *None* | **Internal Only** | Routed through API Gateway |
| **Payment Service** | 8086 | *None* | **Strict Internal** | **Zero Gateway Route**; Feign internal only |
| **Eureka Server** | 8761 | *None* | **Internal Only** | Cluster service discovery |
| **Config Server** | 8888 | *None* | **Internal Only** | Cluster configuration |
| **MySQL Server** | 3306 | *None* | **Internal Only** | Database connections within Docker |
| **Prometheus / Grafana**| 9090, 3000 | *None* | **Admin Private** | Admin access via SSH tunnel (`localhost`) |

---

## 3. CI/CD Deployment Architecture

```
[ Developer Git Push ]
        |
        v
[ GitHub Repository (main branch) ]
        |
        v
[ GitHub Actions Runner ]
        |
        +---> Step 1: Backend Unit & Integration Tests (Java 21, 141 Tests)
        +---> Step 2: Frontend Build & Bundle Compilation (Node 22, Vite)
        +---> Step 3: Validate Docker Compose Specifications
        |
        v (If checks pass & release triggered)
[ Deploy Job via SSH (appleboy/ssh-action) ]
        |
        v (Encrypted SSH Tunnel over Port 22)
[ Production Cloud Virtual Machine ]
        |
        +---> 1. Run scripts/backup-databases.sh (Creates safety snapshot)
        +---> 2. git pull origin main
        +---> 3. docker compose -f docker-compose.prod.yml build
        +---> 4. docker compose -f docker-compose.prod.yml up -d
        +---> 5. Run scripts/health-check.sh (Verifies /healthz, /api/v1/products)
```

---

## 4. Disaster Recovery & Persistence Architecture

1. **Volume Isolation**: Data lives on Docker volume `shopsphere_mysql_prod_data`, backed by encrypted EBS block storage on AWS.
2. **Automated Backups**: `scripts/backup-databases.sh` performs atomic `mysqldump --single-transaction` across all 6 schemas with SHA-256 verification and gzip compression.
3. **Rollback Safety**: Every automated deployment takes a pre-deployment database snapshot before recreating containers.
