# ShopSphere - Containerized Deployment Architecture
**Phase 11 — Part 1: Docker Containerization & Local Production Configuration**

## 1. Architectural Overview

ShopSphere uses an isolated, multi-tiered microservices architecture running inside a dedicated Docker bridge network (`shopsphere_network`). All ingress traffic from client web browsers is mediated through either the high-performance **Nginx Frontend Web Server** or the **Spring Cloud API Gateway**.

Internal microservices communicate securely using Docker internal DNS names and Eureka Service Discovery without direct public exposure.

```
                              [ User Browser ]
                                     |
              +----------------------+----------------------+
              |                                             |
              v (Port 5173 / 80)                            v (Port 8080)
   +---------------------+                       +---------------------+
   |   Frontend Server   |                       |     API Gateway     |
   |   (Nginx 1.27)      |                       |   (Spring Cloud)    |
   |   - React 19 SPA    |                       |   - Perimeter Auth  |
   |   - Proxy /api/ ----+---------------------->|   - Route Filtering |
   +---------------------+                       |   - Header Strip    |
                                                 +----------+----------+
                                                            |
                       +------------------------------------+------------------------------------+
                       |                      |             |                      |             |
                       v                      v             v                      v             v
                +-------------+        +-------------+ +-------------+      +-------------+ +-------------+
                |    Auth     |        |   Product   | |    Cart     |      |    Order    | |  Inventory  |
                |   Service   |        |   Service   | |   Service   |      |   Service   | |   Service   |
                |   (:8081)   |        |   (:8082)   | |   (:8083)   |      |   (:8084)   | |   (:8085)   |
                +------+------+        +------+------+ +------+------+      +------+------+ +------+------+
                       |                      |               |                    |               |
                       |                      |               |                    | (Internal     |
                       |                      |               |                    |  Feign Call)  |
                       |                      |               |                    v               |
                       |                      |               |             +-------------+        |
                       |                      |               |             |   Payment   |        |
                       |                      |               |             |   Service   |<-------+
                       |                      |               |             |   (:8086)   |
                       |                      |               |             |  [INTERNAL] |
                       |                      |               |             +------+------+
                       |                      |               |                    |
+----------------------------------------------------------------------------------+---------------------+
|                                            DATABASE LAYER                                          |
|                                                                                                    |
|                                        [ MySQL 8.0 Engine ]                                        |
|                          (Persistent Volume: shopsphere_mysql_data)                                |
|                                                                                                    |
|   +-------------------+  +-------------------+  +-------------------+  +-----------------------+   |
|   |  shopsphere_auth  |  |shopsphere_product |  |  shopsphere_cart  |  |   shopsphere_order    |   |
|   +-------------------+  +-------------------+  +-------------------+  +-----------------------+   |
|   +-----------------------+                     +-----------------------+                          |
|   | shopsphere_inventory  |                     |  shopsphere_payment   |                          |
|   +-----------------------+                     +-----------------------+                          |
+----------------------------------------------------------------------------------------------------+

+----------------------------------------------------------------------------------------------------+
|                                    INFRASTRUCTURE & OBSERVABILITY                                  |
|                                                                                                    |
|   +-----------------------+  +-----------------------+  +-------------------+  +---------------+   |
|   |     Config Server     |  |   Service Registry    |  |    Prometheus     |  |    Grafana    |   |
|   |        (:8888)        |  |    Eureka (:8761)     |  |      (:9090)      |  |    (:3000)    |   |
|   +-----------------------+  +-----------------------+  +-------------------+  +---------------+   |
|   +-----------------------+                                                                        |
|   |     Zipkin Tracing    |                                                                        |
|   |        (:9411)        |                                                                        |
|   +-----------------------+                                                                        |
+----------------------------------------------------------------------------------------------------+
```

---

## 2. Container Network & Port Mapping Strategy

| Container Service | Internal Port | Host Published Port | Access Level | Rationale |
| :--- | :--- | :--- | :--- | :--- |
| `frontend` | 80 | **5173** (or 80) | **Public** | Primary user storefront entry point |
| `api-gateway` | 8080 | **8080** | **Public** | Public API perimeter for external clients & frontend |
| `service-registry` | 8761 | **8761** | **Admin / Dev** | Service discovery dashboard for cluster inspection |
| `config-server` | 8888 | **8888** | **Admin / Dev** | Spring Cloud configuration inspection |
| `mysql` | 3306 | **3306** | **Internal/Dev**| Database inspection during local development |
| `auth-service` | 8081 | *None (Hidden)* | **Internal** | Accessible solely via API Gateway `/api/v1/auth/**` |
| `product-service` | 8082 | *None (Hidden)* | **Internal** | Accessible solely via API Gateway `/api/v1/products/**` |
| `cart-service` | 8083 | *None (Hidden)* | **Internal** | Accessible solely via API Gateway `/api/v1/cart/**` |
| `order-service` | 8084 | *None (Hidden)* | **Internal** | Accessible solely via API Gateway `/api/v1/orders/**` |
| `inventory-service`| 8085 | *None (Hidden)* | **Internal** | Accessible solely via API Gateway `/api/v1/inventory/**` |
| `payment-service` | 8086 | *None (Hidden)* | **Strict Internal** | **Zero direct public exposure**; called exclusively via Order Service Feign client |
| `prometheus` | 9090 | **9090** | **Admin / Dev** | Metrics collection & time-series analysis |
| `grafana` | 3000 | **3000** | **Admin / Dev** | Observability dashboards |
| `zipkin` | 9411 | **9411** | **Admin / Dev** | Distributed tracing visualization |

---

## 3. Security Boundary & Internal Protections

1. **Header Sanitization at Perimeter**:
   - `StripInternalHeadersGlobalFilter` on API Gateway strips `X-User-Id`, `X-Internal-Service`, and `X-Internal-Secret` from all incoming external HTTP requests.
   - External callers cannot forge user identities or invoke internal endpoints.

2. **Strict Internal Services Isolation**:
   - `payment-service` has no published host port and is unreachable from the host machine outside Docker.
   - Internal endpoints such as `/api/v1/orders/*/recover`, `/api/v1/orders/recover-pending`, and `/api/v1/inventory/reservations/**` are explicitly denied (`denyAll()`) at the API Gateway security filter chain.

3. **Database Isolation**:
   - Dedicated schemas per microservice (`shopsphere_auth`, `shopsphere_product`, `shopsphere_cart`, `shopsphere_order`, `shopsphere_inventory`, `shopsphere_payment`).
   - Zero cross-service database access; zero cross-service foreign keys.
   - Schema migrations managed independently by Flyway inside each microservice.

---

## 4. Production Readiness & Cloud Migration (Part 2)

All microservices are 100% decoupled from local filesystem paths:
- Config Server search locations dynamically configured via `CONFIG_REPO_PATH`.
- Database hosts and credentials injected via `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`.
- Service discovery endpoint configured via `EUREKA_SERVER_URL`.
- Multi-stage Docker builds ensure zero build tools or excess packages in runtime images.
- Non-root user `spring:spring` guarantees least privilege.
- Seamless transition to AWS ECS, Kubernetes (EKS/GKE), or Docker Swarm in Phase 11 Part 2.
