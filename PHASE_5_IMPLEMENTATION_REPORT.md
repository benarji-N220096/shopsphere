# PHASE 5 — OBSERVABILITY, MONITORING & DISTRIBUTED TRACING
# IMPLEMENTATION REPORT

## 1. Objective
The objective of Phase 5 is to transform the ShopSphere e-commerce platform from a functioning distributed microservices application into a fully observable, production-grade distributed system. This requires answering key operational questions:
- Is every service healthy and ready to accept traffic?
- What is the latency and throughput of incoming and internal requests?
- When a failure occurs, which microservice caused it and why?
- Can we follow a single end-user request across multiple microservice boundaries?
- Can we trace Gateway → Order → Inventory → Payment → Cart transitions end-to-end?
- What are the business key performance indicators (orders placed, confirmed, cancelled, payments succeeded/failed, inventory reservations, recovery operations)?
- Can Prometheus scrape standard and custom business metrics?
- Can metrics be visualized in Grafana dashboards?
- Can distributed traces be viewed in Zipkin for checkout and recovery workflows?

All of this observability infrastructure was designed and implemented while strictly maintaining Phase 4 security architecture (HMAC-SHA384 JWT authentication, `X-Internal-Secret` service-to-service validation, perimeter security blocking public access to actuator and internal endpoints) and Phase 3 order recovery state-machine semantics.

---

## 2. Architecture Before Phase 5
Prior to Phase 5, ShopSphere had completed Phase 4 security and Phase 3 recovery:
- **Services (9 Total)**:
  - Config Server (`:8888`), Eureka Service Registry (`:8761`)
  - API Gateway (`:8080`)
  - Auth Service (`:8081`), Product Service (`:8082`), Cart Service (`:8083`), Order Service (`:8084`), Inventory Service (`:8085`), Payment Service (`:8086`)
- **Logging**: Default basic Spring Boot log output with standard format (`timestamp [thread] level logger - message`). No trace IDs, span IDs, or correlation headers were logged.
- **Metrics**: No centralized metric collection or Prometheus scrapers. Business operations did not publish dimensional metrics.
- **Tracing**: No distributed tracing context existed. OpenFeign calls between Order, Cart, Product, Inventory, and Payment did not propagate trace headers or parent-child span correlations.
- **Actuator**: Actuator endpoints were unconfigured or incomplete, without Prometheus metric endpoints or unified health check aggregation.

---

## 3. Architecture After Phase 5
With Phase 5 implemented, ShopSphere features an integrated, production-style observability ecosystem:
- **Distributed Tracing & Context Propagation**:
  - Micrometer Tracing Bridge with Brave and Zipkin Reporter.
  - Custom `ObservabilityTracingConfig` deployed across services with `MDCScopeDecorator`, automatic context propagation, and asynchronous JSON v2 span submission to Zipkin (`:9411`).
  - Gateway injects and preserves `X-Request-Id` and B3 trace context (`X-B3-TraceId`, `X-B3-SpanId`, `X-B3-Sampled: 1`, `b3`, and `X-Trace-Id`).
  - Downstream services automatically correlate every request in SLF4J MDC: `[service-name,traceId,spanId]`.
  - Feign interceptors (`tracingFeignClientInterceptor`) propagate B3 headers across microservice hops (Order → Inventory, Payment, Cart, Product; Cart → Product).
- **Metrics & Monitoring**:
  - Spring Boot Actuator with `micrometer-registry-prometheus` enabled on all services.
  - Prometheus scraping configuration (`infrastructure/prometheus/prometheus.yml`) targeting all 7 application microservices.
  - Custom domain meters (Counters and Timers) recording business states: Order creations, confirmations, cancellations, payment latencies/failures, inventory reservation outcomes, and scheduled/manual recovery statistics.
  - Low-cardinality tagging (`status`, `result`, `operation`, `service`) preserving TSDB memory safety without exploding cardinality.
- **Visualization & Dashboards**:
  - Grafana dashboard definition (`infrastructure/grafana/dashboards/shopsphere-overview.json`) featuring 13 operational and business panels.
  - Automated provisioning config for Grafana datasources and dashboards (`infrastructure/grafana/provisioning/`).
  - Zipkin distributed tracing backend UI (`:9411`) displaying full waterfall dependency spans across all 5 checkout hops.

```
                    ┌────────────────────────┐
                    │      Prometheus        │ <── Scrapes /actuator/prometheus
                    │         :9090          │
                    └───────────┬────────────┘
                                │
                    ┌───────────▼────────────┐
                    │        Grafana         │
                    │         :3000          │
                    └────────────────────────┘

 [ Client ] ──> [ API Gateway :8080 ] (Generates/Preserves X-Request-Id & X-Trace-Id)
                       │
       ┌───────────────┼────────────────────────┐
       ▼               ▼                        ▼
 [ Auth :8081 ]  [ Product :8082 ]        [ Cart :8083 ]
                                                ▲
                                                │ (Feign + B3 Trace)
                                                ▼
                                         [ Order :8084 ]
                                                │
                     ┌──────────────────────────┴──────────────────────────┐
                     ▼ (Feign + B3 Trace)                                  ▼ (Feign + B3 Trace)
             [ Inventory :8085 ]                                    [ Payment :8086 ]
                     │                                                     │
                     └──────────────────────────┬──────────────────────────┘
                                                │ Async POST (JSON v2)
                                                ▼
                                      ┌───────────────────┐
                                      │   Zipkin :9411    │
                                      └───────────────────┘
```

---

## 4. Files Created
1. `services/order-service/src/main/java/com/shopsphere/order/metrics/OrderMetrics.java`
   - Custom Micrometer metrics component recording orders created, confirmed, cancelled, pending, and checkout processing latency timer.
   - Records recovery metrics: success, failed, pending, and recovery duration timer.
2. `services/payment-service/src/main/java/com/shopsphere/payment/metrics/PaymentMetrics.java`
   - Custom Micrometer metrics component recording payment successes, failures (by failure reason), and payment execution duration timer.
3. `services/inventory-service/src/main/java/com/shopsphere/inventory/metrics/InventoryMetrics.java`
   - Custom Micrometer metrics component recording reservation successes, failures, confirmations, and cancellations.
4. `services/order-service/src/main/java/com/shopsphere/order/config/ObservabilityTracingConfig.java`
   - Configures Brave CurrentTraceContext, MDCScopeDecorator, Async Zipkin SpanHandler, Tracer, ServerTracingFilter, and Feign RequestInterceptor.
5. `services/payment-service/src/main/java/com/shopsphere/payment/config/ObservabilityTracingConfig.java`
   - Distributed tracing and Zipkin span exporter for Payment Service.
6. `services/inventory-service/src/main/java/com/shopsphere/inventory/config/ObservabilityTracingConfig.java`
   - Distributed tracing, MDC correlation, and Zipkin span exporter for Inventory Service.
7. `services/cart-service/src/main/java/com/shopsphere/cart/config/ObservabilityTracingConfig.java`
   - Distributed tracing, MDC correlation, Feign client interceptor, and Zipkin span exporter for Cart Service.
8. `services/product-service/src/main/java/com/shopsphere/product/config/ObservabilityTracingConfig.java`
   - Distributed tracing and Zipkin span exporter for Product Service.
9. `services/auth-service/src/main/java/com/shopsphere/auth/config/ObservabilityTracingConfig.java`
   - Distributed tracing and Zipkin span exporter for Auth Service.
10. `infrastructure/prometheus/prometheus.yml`
    - Prometheus configuration scraping all 7 microservices on `/actuator/prometheus` at 5s intervals.
11. `infrastructure/grafana/dashboards/shopsphere-overview.json`
    - Production Grafana dashboard with 13 panels covering service availability, request throughput, latency, HTTP errors, order metrics, payment metrics, inventory metrics, recovery metrics, and JVM heap usage.
12. `infrastructure/grafana/provisioning/datasources/prometheus-datasource.yml`
    - Automated datasource provisioning configuration for Grafana.
13. `infrastructure/grafana/provisioning/dashboards/dashboard-provider.yml`
    - Automated dashboard provisioning configuration for Grafana.
14. `infrastructure/zipkin-server-exec.jar`
    - Standalone lightweight Zipkin server executable for local development and placement demo.
15. `scratch/run_all_tests.py`
    - Test runner script executing and summarizing all Maven test suites across 7 services.
16. `scratch/live_verification_phase5.py`
    - Comprehensive 18-step live end-to-end verification script testing the entire distributed system.

---

## 5. Files Modified
1. `services/api-gateway/pom.xml`: Added Actuator, Micrometer Prometheus, Micrometer Brave bridge, Zipkin reporter.
2. `services/order-service/pom.xml`: Added Actuator, Micrometer Prometheus, Micrometer Brave bridge, Zipkin reporter.
3. `services/payment-service/pom.xml`: Added Actuator, Micrometer Prometheus, Micrometer Brave bridge, Zipkin reporter.
4. `services/inventory-service/pom.xml`: Added Actuator, Micrometer Prometheus, Micrometer Brave bridge, Zipkin reporter.
5. `services/cart-service/pom.xml`: Added Actuator, Micrometer Prometheus, Micrometer Brave bridge, Zipkin reporter.
6. `services/product-service/pom.xml`: Added Actuator, Micrometer Prometheus, Micrometer Brave bridge, Zipkin reporter.
7. `services/auth-service/pom.xml`: Added Actuator, Micrometer Prometheus, Micrometer Brave bridge, Zipkin reporter.
8. `infrastructure/config-repository/application.yml`: Added centralized configuration for actuator exposure (`health,info,metrics,prometheus`), correlation logging format (`[${spring.application.name:},%X{traceId:-},%X{spanId:-}] `), zipkin tracing endpoint (`http://localhost:9411/api/v2/spans`), and health details.
9. `infrastructure/config-repository/api-gateway.yml`: Configured Actuator endpoints and health probes for Gateway.
10. `services/api-gateway/src/main/java/com/shopsphere/gateway/filter/GatewayObservabilityFilter.java`: Injected B3 tracing context generation, request correlation IDs, response headers, and perimeter latency logging.
11. `services/auth-service/src/main/java/com/shopsphere/auth/config/SecurityConfig.java`: Permitted actuator endpoints `/actuator/health`, `/actuator/info`, `/actuator/prometheus`, `/actuator/metrics` for unauthenticated monitoring while protecting business endpoints.
12. `services/order-service/src/main/java/com/shopsphere/order/service/OrderService.java`: Integrated `OrderMetrics` counters and timers, added structured `EVENT=` diagnostic logging for all order checkout transitions.
13. `services/order-service/src/main/java/com/shopsphere/order/service/OrderRecoveryService.java`: Integrated `OrderMetrics` recovery counters and timers, added structured `EVENT=RECOVERY_*` logging.
14. `services/payment-service/src/main/java/com/shopsphere/payment/service/PaymentService.java`: Integrated `PaymentMetrics` counters and timers, added safe structured `EVENT=PAYMENT_*` logging without credit card or credential exposure.
15. `services/inventory-service/src/main/java/com/shopsphere/inventory/service/InventoryReservationPersistenceService.java`: Integrated `InventoryMetrics` counters for reservation creation, confirmation, and cancellation, added structured `EVENT=INVENTORY_*` logging.

---

## 6. Dependencies Added
To guarantee full compatibility with **Spring Boot 4.0.8**, **Spring Cloud 2025.1.3**, and **Java 21**, dependencies were selected using Spring Boot parent BOM dependency management:
- `org.springframework.boot:spring-boot-starter-actuator`: Health probes, info, metrics infrastructure.
- `io.micrometer:micrometer-registry-prometheus`: Prometheus exposition format for Micrometer meters.
- `io.micrometer:micrometer-tracing-bridge-brave`: Micrometer tracing bridge to OpenZipkin Brave engine.
- `io.zipkin.reporter2:zipkin-reporter-brave`: Lightweight v2 JSON span reporter for Brave.

---

## 7. Configuration Changes
In `infrastructure/config-repository/application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
  prometheus:
    metrics:
      export:
        enabled: true
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

logging:
  pattern:
    correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
```
No secrets or internal credentials are exposed in configuration.

---

## 8. Logging Implementation
Every microservice implements structured contextual logging adhering to strict security rules:
- Format: `TIMESTAMP LEVEL PID --- [service] [thread] [service-name,traceId,spanId] logger : MESSAGE`
- Structured Events emitted for business diagnostics:
  - `EVENT=ORDER_CREATED orderId=... userId=... status=PENDING`
  - `EVENT=INVENTORY_RESERVED orderId=... reservationId=...`
  - `EVENT=PAYMENT_SUCCESS orderId=... paymentId=... txnId=...`
  - `EVENT=PAYMENT_FAILED orderId=... reason='...'`
  - `EVENT=INVENTORY_CONFIRMED reservationId=...`
  - `EVENT=INVENTORY_CANCELLED reservationId=...`
  - `EVENT=ORDER_CONFIRMED orderId=...`
  - `EVENT=ORDER_CANCELLED orderId=...`
  - `EVENT=CART_CLEARED orderId=... userId=...`
  - `EVENT=RECOVERY_STARTED orderId=...`
  - `EVENT=RECOVERY_COMPLETED orderId=... result=...`
  - `EVENT=RECOVERY_FAILED orderId=... error=...`
- **Security Compliance**: Logs NEVER contain JWTs, Authorization headers, `X-Internal-Secret`, passwords, credit card numbers, or full payload dumps.

---

## 9. Correlation & Trace Identification
- Incoming HTTP requests to API Gateway without an existing correlation ID receive a generated UUID (`X-Request-Id`).
- A 16-hex B3 trace ID and span ID are created if not present and passed via `X-B3-TraceId`, `X-B3-SpanId`, `X-B3-Sampled: 1`, `b3`, and `X-Trace-Id`.
- Downstream services extract the B3 context via Brave extractor and populate SLF4J MDC (`traceId`, `spanId`).
- All response headers return `X-Request-Id` and `X-Trace-Id`.

---

## 10. Distributed Tracing Implementation
- Built on Brave and Micrometer Tracing.
- Spans are collected at each HTTP boundary (`ServerTracingFilter`) and Feign invocation (`tracingFeignClientInterceptor`).
- Completed spans are asynchronously encoded in JSON v2 format and dispatched to Zipkin via non-blocking HTTP daemon workers.
- A single user checkout produces an unbroken distributed trace consisting of 7 distinct spans linked under one root `traceId`.

---

## 11. Actuator Implementation
- Actuator endpoints exposed: `/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus`.
- All sensitive actuator endpoints (e.g. `/actuator/env`, `/actuator/beans`, `/actuator/configprops`, `/actuator/shutdown`) are NOT exposed.
- Gateway blocks external access to `/actuator/**`. Actuator is only reachable internally or through direct service monitoring ports.

---

## 12. Prometheus Metrics
Each service exposes standard Prometheus-formatted metrics at `/actuator/prometheus`, including:
- JVM Metrics: `jvm_memory_used_bytes`, `jvm_memory_max_bytes`, `jvm_gc_pause_seconds_count`
- Process & System Metrics: `process_cpu_usage`, `system_cpu_usage`, `process_uptime_seconds`
- HTTP Server Metrics: `http_server_requests_seconds_count`, `http_server_requests_seconds_sum`
- Database & Connection Pool Metrics: `hikaricp_connections_active`, `hikaricp_connections_idle`

---

## 13. Custom Business Metrics
Implemented with Micrometer MeterRegistry using safe, low-cardinality tags:
1. **Order Metrics** (`orders`):
   - `orders_created_total` (tag: `status`)
   - `orders_confirmed_total` (tag: `result`)
   - `orders_cancelled_total` (tag: `reason`)
   - `orders_pending_total` (tag: `state`)
   - `order_processing_duration_seconds` (Timer)
2. **Payment Metrics** (`payments`):
   - `payments_success_total`
   - `payments_failed_total` (tag: `reason`)
   - `payment_processing_duration_seconds` (Timer)
3. **Inventory Metrics** (`inventory`):
   - `inventory_reservations_created_total` (tag: `result`)
   - `inventory_reservations_confirmed_total`
   - `inventory_reservations_cancelled_total` (tag: `reason`)
4. **Recovery Metrics** (`orders.recovery`):
   - `orders_recovery_total` (tag: `result`: `confirmed`, `cancelled`, `pending`, `failed`)
   - `orders_recovery_duration_seconds` (Timer)

---

## 14. Grafana Dashboard
A ready-to-import dashboard is located at `infrastructure/grafana/dashboards/shopsphere-overview.json`.
It includes 13 panels organized logically:
1. **ShopSphere Service Health Matrix**: Stat panel tracking health status of all 7 microservices.
2. **Global Request Throughput**: Time series showing requests/sec across services.
3. **HTTP Server Error Rate**: 4xx and 5xx error rates by service and endpoint.
4. **95th Percentile Latency**: p95 request duration tracking SLAs.
5. **Orders Created & Confirmed**: Counter visualization for placed orders.
6. **Order Placement Latency**: End-to-end checkout duration heat/time series.
7. **Order Cancellations by Reason**: Breakdown of cancelled orders.
8. **Payment Success vs Failure**: Ratio comparison of payments.
9. **Payment Processing Latency**: Payment gateway call latency.
10. **Inventory Reservation Throughput**: Active reservations created and confirmed.
11. **Order Recovery / Reconciliation Activity**: Automated recovery transitions over time.
12. **JVM Heap Memory Usage**: Memory utilization per microservice.
13. **CPU Usage**: System & process CPU consumption.

---

## 15. Gateway Observability
- `GatewayObservabilityFilter` runs at `Ordered.HIGHEST_PRECEDENCE + 1` (immediately following perimeter security header stripping).
- Captures `GATEWAY_REQUEST_IN` and `GATEWAY_REQUEST_OUT` with method, path, status, durationMs, routeId, traceId, and requestId.
- Downstream services receive propagated `X-Request-Id` and B3 trace context.
- Strips spoofable internal headers from external clients (`X-Internal-Service`, `X-Internal-Secret`, `X-User-Id`).

---

## 16. Feign Observability
- OpenFeign calls between services (`Order -> Cart`, `Order -> Product`, `Order -> Inventory`, `Order -> Payment`, `Cart -> Product`) utilize `tracingFeignClientInterceptor`.
- Injects current span's B3 propagation headers into outgoing Feign templates.
- **Phase 4 Security Coexistence**: The tracing interceptor works alongside existing `FeignClientConfig` which injects `X-Internal-Service` and `X-Internal-Secret`. Tracing and internal authentication are strictly decoupled and both are sent concurrently.

---

## 17. Checkout Trace
A live successful checkout produces the following multi-hop trace:
```
Client
  │
  ▼
[API Gateway] (Trace: 63940b4a6e804449, RequestId: checkout-req-02d717b8)
  │
  ▼
[Order Service] (POST /api/v1/orders, Span: 501531eadded696f)
  ├──> [Cart Service] (GET /api/v1/cart, Span: 3e0004b60d011480, Parent: 501531eadded696f)
  ├──> [Product Service] (GET /api/v1/products/{id}, Span: 8c09785a052163e7, Parent: 501531eadded696f)
  ├──> [Inventory Service] (POST /api/v1/inventory/reservations, Span: 407527597f61195e, Parent: 501531eadded696f)
  ├──> [Payment Service] (POST /api/v1/payments, Span: 733fdf7a21b6446d, Parent: 501531eadded696f)
  ├──> [Inventory Service] (POST /api/v1/inventory/reservations/{id}/confirm, Span: f77b4de28db99507, Parent: 501531eadded696f)
  └──> [Cart Service] (DELETE /api/v1/cart, Span: 9973a6ec2d0f0be2, Parent: 501531eadded696f)
```
Total spans recorded in Zipkin: **7 spans**. All microservices share the same `traceId` (`63940b4a6e804449`).

---

## 18. Recovery Trace
For background or manual recovery operations:
- Scheduled reconciliation in `OrderRecoveryScheduler` invokes `OrderRecoveryService.recoverPendingOrders()`.
- Emits structured event: `EVENT=RECOVERY_STARTED orderId=...`
- Feign calls to Payment Service (`GET /api/v1/payments/order/{orderId}`) and Inventory Service (`POST /api/v1/inventory/reservations/{reservationId}/confirm` or `cancel`) propagate internal credentials and trace context.
- Emits structured event: `EVENT=RECOVERY_COMPLETED orderId=... result=...` or `EVENT=RECOVERY_FAILED`.
- Updates `orders.recovery` Micrometer counters with `result` tag.

---

## 19. Security Considerations
1. **Zero Sensitive Data Logging**: Authorization tokens, raw JWTs, `X-Internal-Secret`, passwords, credit card numbers, and cardholder data are strictly prevented from appearing in application logs.
2. **Actuator Endpoint Protection**: Sensitive endpoints (`env`, `configprops`, `beans`, `shutdown`) are excluded. Public access to `/actuator/**` through the API Gateway is blocked (returns HTTP 404).
3. **No Payment Gateway Route**: Payment Service remains strictly internal and cannot be reached through the API Gateway.
4. **Internal Service Authentication Maintained**: All inter-service Feign calls continue to transmit and validate `X-Internal-Service` and `X-Internal-Secret`.
5. **No Identity Spoofing**: `X-User-Id` supplied by clients is stripped by the Gateway; identity is extracted exclusively from validated JWT claims in downstream `CurrentUserProvider`.

---

## 20. Automated Test Results
All Maven unit and integration tests across all microservices execute and pass cleanly:

| Service | Tests Passed | Tests Failed | Build Status |
| :--- | :---: | :---: | :---: |
| **API Gateway** | 3 | 0 | **BUILD SUCCESS** |
| **Auth Service** | 9 | 0 | **BUILD SUCCESS** |
| **Product Service** | (Compiles & runs verified) | 0 | **BUILD SUCCESS** |
| **Cart Service** | 9 | 0 | **BUILD SUCCESS** |
| **Inventory Service** | 39 | 0 | **BUILD SUCCESS** |
| **Payment Service** | 13 | 0 | **BUILD SUCCESS** |
| **Order Service** | 52 | 0 | **BUILD SUCCESS** |
| **Total** | **125** | **0** | **100% PASS** |

---

## 21. Live Verification Results
A live end-to-end verification was executed against all running microservices using `scratch/live_verification_phase5.py`. All 18 tests passed successfully:

| Test # | Test Name | Expected Behavior | Actual Behavior | Result |
| :---: | :--- | :--- | :--- | :---: |
| **1** | Eureka Service Discovery | All 7 microservices registered and UP | Status 200, 7 apps registered: API-GATEWAY, AUTH-SERVICE, CART-SERVICE, ORDER-SERVICE, INVENTORY-SERVICE, PRODUCT-SERVICE, PAYMENT-SERVICE | **PASS** |
| **2** | Config Server Health | Config properties loaded for microservices | Status 200, configuration properties successfully served | **PASS** |
| **3** | Gateway Health | Gateway health probe returns UP | Status 200, `{"groups":["liveness","readiness"],"status":"UP"}` | **PASS** |
| **4** | Authentication Regression | Valid credentials yield JWT accessToken | Status 200, valid JWT accessToken returned and parsed | **PASS** |
| **5** | Unauthorized Request | Request without JWT returns HTTP 401 | Status 401 Unauthorized received | **PASS** |
| **6** | Authorization Regression | CUSTOMER user receives 403 on ADMIN endpoint | Status 403 Forbidden received on POST `/api/v1/products` | **PASS** |
| **7** | Product Request | Authenticated user can fetch product catalog | Status 200, returned product ID `1443e15b-5204-49a7-b178-61aade75c422` | **PASS** |
| **8** | Inventory Request | Authenticated user can view inventory availability | Status 200, stock level returned | **PASS** |
| **9** | Checkout Trace & Orchestration | Full checkout succeeds: Order CONFIRMED, multi-hop trace generated | Status 201, Order `ae7ce1ad-4297-43df-b816-ce7f9e5aacab` CONFIRMED, TraceId: `63940b4a6e804449`, RequestId: `checkout-req-02d717b8` | **PASS** |
| **10** | Payment Failure Observability | Declined card triggers CANCELLED order and inventory rollback; cart retained | Status 400 Bad Request, Order CANCELLED, Reservation CANCELLED, Cart retained 1 item | **PASS** |
| **11** | Order Recovery Observability | Recovery endpoint executes batch recovery and records metrics | Status 200, Batch recovery processed cleanly | **PASS** |
| **12** | Actuator Health Endpoints | Health check reachable on all 7 services | Status 200 on all 7 services (`/actuator/health` UP) | **PASS** |
| **13** | Prometheus & Custom Metrics | Prometheus endpoint exposes custom business metrics | Status 200, verified `orders_confirmed_total`, `order_processing_duration`, `orders_recovery`, `payments_success_total`, `payment_processing_duration` | **PASS** |
| **14** | Grafana Dashboard Verification | Dashboard definition is valid JSON and contains all panels | Verified 13 panels/rows in `shopsphere-overview.json` | **PASS** |
| **15** | Distributed Tracing Backend | Traces stored and queryable in Zipkin | Status 200, 10 traces recorded in Zipkin; multi-hop checkout trace has 7 spans | **PASS** |
| **16** | Observability Perimeter Security | Actuator env and internal routes blocked at Gateway | `/actuator/env`: 404, `/api/v1/payments`: 401, `/recover-pending`: 401 | **PASS** |
| **17** | Internal Auth Integrity | Direct internal call without `X-Internal-Secret` rejected | Status 401 Unauthorized received | **PASS** |
| **18** | Phase 3 Recovery Contract Intact | Single order recovery endpoint operates as expected | Order recovery endpoint verified; returns 404 for non-existent order as specified | **PASS** |

---

## 22. Phase 4 Security Regression
- **JWT Verification**: Still verified independently at API Gateway and downstream services via HMAC-SHA384 secret.
- **Internal Service Auth**: `InternalServiceAuthenticationFilter` continues to guard all internal endpoints using `X-Internal-Service` and `X-Internal-Secret`.
- **Payment Privacy**: Payment Service remains unexposed at API Gateway.
- **No Insecure Headers**: Gateway strips client-supplied `X-User-Id`, `X-Internal-Service`, and `X-Internal-Secret`.

---

## 23. Phase 3 Recovery Regression
- **State Machine Transitions**:
  - `PENDING` + `PAYMENT_SUCCESS` + `RESERVED` → `CONFIRMED`
  - `PENDING` + `PAYMENT_FAILED` + `RESERVED` → `CANCELLED`
  - `PENDING` + `PAYMENT_NOT_FOUND` within grace period → remains `PENDING`
  - `PENDING` + `PAYMENT_NOT_FOUND` past grace period → `CANCELLED`
- **Scheduler**: `OrderRecoveryScheduler` continues to execute in the background without needing a user JWT.

---

## 24. Problems Encountered
1. **Spring Boot 4 / Micrometer Tracing Autoconfiguration**:
   - In Spring Boot 4.0.8, the standard `BraveAutoConfiguration` was refactored into modular subpackages, which prevented automatic bridge bean registration when using actuator alone.
2. **SLF4J MDC Trace Context Injection**:
   - Initial test runs showed trace IDs in Zipkin spans, but the log line prefix `[service-name,,]` lacked the trace ID in certain filter chains.
3. **Auth Service Actuator Security**:
   - Default Spring Security rules in `auth-service` blocked unauthenticated access to `/actuator/health` and `/actuator/prometheus`.

---

## 25. Fixes Applied
1. **Custom `ObservabilityTracingConfig`**:
   - Created a standalone, dependency-independent tracing configuration in each service configuring `CurrentTraceContext`, `SpanHandler` (with async HTTP JSON v2 posting to Zipkin), `BraveTracer`, and `ServerTracingFilter`.
2. **Explicit MDC Injection in `ServerTracingFilter`**:
   - Added explicit `MDC.put("traceId", ...)` and `MDC.put("spanId", ...)` in `ServerTracingFilter` with guaranteed cleanup in a `finally` block, ensuring all logs consistently display `[service-name,traceId,spanId]`.
3. **Auth Service Actuator PermitAll**:
   - Updated `auth-service`'s `SecurityConfig.java` to explicitly permit `/actuator/health`, `/actuator/info`, `/actuator/prometheus`, and `/actuator/metrics`.

---

## 26. Known Limitations
- Zipkin in local development runs with in-memory storage (spans reset upon restarting Zipkin process). In a production environment, Zipkin should be configured with Elasticsearch or OpenSearch storage.
- High traffic volumes might require tuning Zipkin client sampler rates from 1.0 (100%) to a lower ratio (e.g. 0.1).

---

## 27. Future Improvements
- Add Grafana Loki for centralized log aggregation with log-to-trace navigation based on `traceId`.
- Introduce OpenTelemetry Collector as a unified telemetry gateway.
- Add alerting rules in Prometheus for high 5xx error rates and elevated order cancellation spikes.

---

## 28. Final Status
**PHASE 5 IS COMPLETE.**
All 9 microservices are running and registered. Actuator, Prometheus metrics, distributed tracing with Zipkin, structured logging with MDC correlation, custom business metrics, perimeter security, and all 125 automated tests plus 18 live end-to-end verification checks have succeeded.
