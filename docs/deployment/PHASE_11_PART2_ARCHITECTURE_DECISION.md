# ShopSphere — Architecture Decision Record (ADR)
## Phase 11 Part 2: Cloud Deployment Architecture Selection

**Document ID:** ADR-011-P2-01  
**Status:** ACCEPTED  
**Date:** October 2026  
**Author:** ShopSphere DevOps & Cloud Architecture Team  

---

## 1. Context & Problem Statement

ShopSphere is an enterprise-grade e-commerce application consisting of:
- **9 Spring Boot Microservices** (API Gateway, Auth, Product, Cart, Order, Inventory, Payment, Eureka Service Registry, Spring Cloud Config Server) running Java 21.
- **1 React Storefront** (React 19 / Vite SPA) served via Nginx.
- **1 MySQL 8.0 Database** hosting 6 independent service-owned schemas.
- **Observability Components** (Prometheus, Grafana, Zipkin).

The goal of Phase 11 Part 2 is to deploy ShopSphere to a cloud environment that is:
1. **Cost-Conscious & Practical**: Suitable for a student/placement portfolio project without incurring unmanageable ongoing cloud bills ($20–$50/month target).
2. **Architecturally Faithful**: Preserving Spring Cloud Gateway, Eureka service discovery, Spring Cloud Config Server, and strict microservice schema ownership.
3. **Secure & Production-Ready**: Enforcing perimeter HTTPS/TLS, strict firewall rules (internal services hidden), no hardcoded secrets, and least-privilege access.
4. **Reproducible & Maintainable**: Automated via CI/CD, Infrastructure-as-Code (Terraform), and reproducible Docker Compose orchestration.

---

## 2. Evaluation of Architectural Options

| Evaluation Criteria | Option A: Single Cloud VM with Docker Compose & Nginx TLS | Option B: Managed Container Hosting (AWS ECS / Fargate) | Option C: Managed Kubernetes (AWS EKS / GKE / AKS) |
| :--- | :--- | :--- | :--- |
| **Monthly Cost** | **$24 – $48 / mo** (e.g., 8 GB RAM / 4 vCPU on Hetzner / DigitalOcean / AWS t3.xlarge Spot) | **$120 – $250+ / mo** (11 continuous container tasks + NAT Gateway + ALB) | **$180 – $350+ / mo** ($73/mo cluster fee + node pool + managed load balancers) |
| **Architectural Compatibility** | **100% Native**: Existing Eureka, Config Server, and Docker networking run identically to Part 1. | **Moderate**: Requires AWS Cloud Map or custom DNS; Config Server repo mounting requires AWS EFS or S3 sync. | **Complex**: Eureka is redundant with K8s DNS/CoreDNS; Config Server redundant with ConfigMaps. |
| **Resource Footprint** | Fits inside 8–16 GB RAM with JVM `-XX:MaxRAMPercentage=60.0` or `-Xmx384m` per service. | Each microservice needs minimum 0.5 vCPU / 1GB RAM = 11 GB total allocated RAM. | High control plane overhead; nodes need at least 16 GB allocatable RAM. |
| **Deployment Complexity** | **Low to Moderate**: Standard Docker Compose, GitHub Actions SSH runner, Certbot. | **High**: Task definitions, IAM roles per task, ECS services, ALB target groups, CloudWatch. | **Very High**: Helm charts, Ingress controller, cert-manager, CSI drivers for MySQL storage. |
| **Suitability for Portfolio** | **Excellent**: Clear, understandable, demonstrable, easy for recruiters to clone and run. | **Moderate**: Cloud-provider locked to AWS; high ongoing costs. | **Overkill**: Unjustified operational cost for an educational microservice portfolio. |

---

## 3. Decision

**Selected Architecture: Option A — Single Appropriately Sized Cloud Virtual Machine Running Docker Compose with Production Nginx Reverse Proxy and Automated TLS.**

### Rationale:
1. **Preservation of Core Architecture**: ShopSphere's Phase 1–10 design centers on Spring Cloud (Eureka Service Discovery, Spring Cloud Config Server, Spring Cloud Gateway). Running on an integrated Docker bridge network allows Eureka and Config Server to function precisely as designed, without forcing an unneeded architectural rewrite or introducing redundant Kubernetes control planes.
2. **Cost-Efficiency & Student Affordability**: A cloud instance with 4 vCPUs and 8–16 GB RAM costs:
   - **Hetzner Cloud (CPX41)**: ~€16.40/month (~$18 USD/month) for 8 vCPUs, 16 GB RAM, 240 GB NVMe SSD.
   - **DigitalOcean Droplet**: ~$48/month for 4 vCPUs, 8 GB RAM.
   - **AWS EC2 (t3.xlarge)**: ~$120/month on-demand, or ~$36/month on Spot/Savings Plans.
   In contrast, managed Kubernetes (EKS) costs $73/month just for the cluster control plane before spinning up a single worker node.
3. **Security Boundaries Preserved**: Only ports 80 (HTTP) and 443 (HTTPS) are exposed to the public internet. All microservices, Eureka, Config Server, and MySQL remain strictly bound to the private internal Docker bridge network (`shopsphere_prod_network`).
4. **Persistent Data Integrity**: MySQL data persists on an attached persistent volume (`shopsphere_mysql_data`), decoupled from container lifecycles.
5. **Portability**: The entire stack can be launched on ANY cloud provider (AWS, GCP, Azure, DigitalOcean, Hetzner, Linode) or on-premises server using identical commands.

---

## 4. Architectural Specifications

### 4.1 Topology Diagram

```
[ User Web Browser ]
        |
        v  HTTPS (Port 443) / HTTP (Port 80 -> Redirect)
+-----------------------------------------------------------------------------------+
|  Cloud Virtual Machine (Ubuntu 24.04 LTS / 8-16 GB RAM / 4 vCPUs)                 |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  | Production Nginx Reverse Proxy (SSL Termination / Certbot Let's Encrypt)    |  |
|  | - /            -> Frontend Web Container (Port 80)                          |  |
|  | - /api/        -> API Gateway (Port 8080)                                   |  |
|  | - Strict Rate Limiting, Security Headers (HSTS, CSP, X-Frame-Options)       |  |
|  +-----------------------------------------------------------------------------+  |
|                                         |                                         |
|  ====================== [ Private Docker Bridge Network ] ======================  |
|                                         |                                         |
|             +---------------------------+---------------------------+             |
|             |                                                       |             |
|             v                                                       v             |
|      +---------------+                                       +---------------+    |
|      |   frontend    | (Port 80)                             |  api-gateway  |    |
|      | (React Vite)  |                                       | (Port 8080)   |    |
|      +---------------+                                       +-------+-------+    |
|                                                                      |            |
|             +--------------------------------------------------------+            |
|             |               |                |               |       |            |
|             v               v                v               v       v            |
|      +-------------+ +-------------+  +-------------+ +-------------+ +---------+ |
|      |auth-service | |product-serv |  |cart-service | |order-service| |inventory| |
|      |  (:8081)    | |  (:8082)    |  |  (:8083)    | |  (:8084)    | | (:8085) | |
|      +------+------+ +------+------+  +------+------+ +------+------+ +----+----+ |
|             |               |                |               | (Feign)     |      |
|             |               |                |               v             |      |
|             |               |                |        +-------------+      |      |
|             |               |                |        |payment-serv |<-----+      |
|             |               |                |        |  (:8086)    |             |
|             |               |                |        | [INTERNAL]  |             |
|             |               |                |        +------+------+             |
|             |               |                |               |                    |
|             +---------------+----------------+---------------+                    |
|                                     |                                             |
|                                     v                                             |
|                             +---------------+                                     |
|                             |     mysql     | (:3306)                             |
|                             |  (MySQL 8.0)  | Volume: shopsphere_mysql_data       |
|                             +---------------+                                     |
|                                                                                   |
|  Infrastructure:                                                                  |
|  - service-registry (Eureka :8761)                                                |
|  - config-server (:8888, mounting /app/config-repository)                         |
|  Observability:                                                                   |
|  - prometheus (:9090), grafana (:3000), zipkin (:9411) [Protected]               |
+-----------------------------------------------------------------------------------+
```

### 4.2 Resource Allocation & JVM Sizing

To ensure all 9 Spring Boot microservices, MySQL, Nginx, and observability run reliably on an 8–16 GB RAM host without OOM kill:

| Component | Heap Min (`-Xms`) | Heap Max (`-Xmx`) | Container RAM Limit | CPU Quota |
| :--- | :--- | :--- | :--- | :--- |
| **API Gateway** | 192 MB | 384 MB | 512 MB | 0.75 vCPU |
| **Auth Service** | 192 MB | 384 MB | 512 MB | 0.50 vCPU |
| **Product Service** | 192 MB | 384 MB | 512 MB | 0.50 vCPU |
| **Cart Service** | 192 MB | 384 MB | 512 MB | 0.50 vCPU |
| **Order Service** | 256 MB | 512 MB | 640 MB | 0.75 vCPU |
| **Inventory Service**| 192 MB | 384 MB | 512 MB | 0.50 vCPU |
| **Payment Service** | 192 MB | 384 MB | 512 MB | 0.50 vCPU |
| **Config Server** | 128 MB | 256 MB | 384 MB | 0.25 vCPU |
| **Service Registry** | 128 MB | 256 MB | 384 MB | 0.25 vCPU |
| **MySQL 8.0** | N/A (Buffer Pool 512 MB) | N/A | 1024 MB | 1.00 vCPU |
| **Frontend Nginx** | N/A | N/A | 128 MB | 0.25 vCPU |
| **Nginx Reverse Proxy**| N/A | N/A | 128 MB | 0.25 vCPU |
| **Prometheus** | N/A | N/A | 256 MB | 0.25 vCPU |
| **Grafana** | N/A | N/A | 256 MB | 0.25 vCPU |
| **Zipkin** | 128 MB | 256 MB | 384 MB | 0.25 vCPU |
| **TOTALS** | **~1.7 GB** | **~3.6 GB** | **~6.5 GB** | **Fits in 8–16 GB Host** |

---

## 5. Security & Boundary Architecture

1. **Firewall / Security Group**:
   - Ingress: Port 80 (HTTP), Port 443 (HTTPS), Port 22 (SSH - restricted to admin IP).
   - All other ports (3306, 8080-8086, 8761, 8888, 9090, 9411) are blocked from external ingress.
2. **Reverse Proxy SSL Termination**:
   - Let's Encrypt automated TLS via Certbot.
   - HTTP -> HTTPS 301 redirection.
   - Security headers: HSTS (`Strict-Transport-Security`), `X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`.
3. **Database Safeguards**:
   - Least-privilege schema ownership: separate users for each microservice.
   - Volume persistence with automated daily database dumps to encrypted storage.
4. **Internal Secrets**:
   - `JWT_SECRET` and `INTERNAL_SERVICE_SECRET` injected via environment variables on the host.
   - `StripInternalHeadersGlobalFilter` strips spoofed identity headers at the API Gateway perimeter.
   - `payment-service` has no public Gateway route and no published host port.

---

## 6. Future Scaling Path (Post Phase 11)

If user traffic scales significantly:
1. **Database Decoupling**: Move MySQL from local container to managed multi-AZ database (e.g., AWS RDS MySQL or DigitalOcean Managed Database).
2. **Horizontal Scaling**: Migrate microservices to AWS ECS Fargate or Kubernetes with Horizontal Pod Autoscaler (HPA) using the standardized Dockerfiles created in Part 1.
3. **CDN Integration**: Place Cloudflare or AWS CloudFront in front of static frontend assets for global edge caching.
