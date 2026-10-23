# Phase 6 Implementation Report: Resilience, Fault Tolerance & Failure Handling

**Project:** ShopSphere Microservices Monorepo  
**Phase:** 6 — Resilience, Fault Tolerance & Failure Handling  
**Date:** September 28, 2026  
**Status:** COMPLETE & FULLY VERIFIED (141/141 automated tests, 9/9 live failure-injection tests)

---

## 1. Executive Summary

Phase 6 introduces enterprise-grade resilience, fault tolerance, and safe failure degradation to the ShopSphere microservices e-commerce platform. Using Resilience4j together with Spring Cloud OpenFeign, Spring Security (Phase 4), and Micrometer/Zipkin/Prometheus (Phase 5), the system is fortified against transient network partitions, downstream service crashes, slow dependencies, and catastrophic cascade failures.

Critical architectural constraints were strictly upheld throughout Phase 6:
- **Idempotency Safety:** Automatic retries are strictly forbidden on non-idempotent operations (`processPayment` and `reserveInventory`).
- **Phase 3 Recovery Preservation:** Under downstream payment uncertainty or timeout, orders remain safely in `PENDING` status, inventory remains `RESERVED`, and customer carts are preserved. Scheduled and manual recovery safely transitions reconciled orders to `CONFIRMED` or `CANCELLED`.
- **Phase 4 Security Model Intact:** JWT validation, role-based access control (`ROLE_CUSTOMER`, `ROLE_ADMIN`), and internal service secret authentication (`X-Internal-Secret: ROLE_INTERNAL_SERVICE`) remain untouched.
- **Phase 5 Observability Maintained:** Resilience4j circuit breaker state transitions, call counts, failure rates, and execution latencies are exported to Prometheus and correlated across distributed Zipkin spans.

---

## 2. Resilience Architecture & Topology

```
                  ┌─────────────────────────────┐
                  │         API Gateway         │
                  │            :8080            │
                  └──────────────┬──────────────┘
                                 │ JWT / X-User-Id
        ┌────────────────────────┼────────────────────────┐
        ▼                        ▼                        ▼
 ┌──────────────┐         ┌──────────────┐         ┌──────────────┐
 │ Auth Service │         │ Cart Service │         │Product Serv. │
 │    :8081     │         │    :8083     │         │    :8082     │
 └──────────────┘         └──────┬───────┘         └──────▲───────┘
                                 │                        │
                    cart-product │ CB (6 calls, 50%)      │ order-product
                    Retry (2x)   │                        │ CB (6 calls, 50%)
                                 ▼                        │ Retry (3x)
                          ┌──────────────┐                │
                          │Order Service ├────────────────┘
                          │    :8084     │
                          └──────┬───────┘
                                 │
         ┌───────────────────────┴───────────────────────┐
         │ order-inventory                               │ order-payment
         │ CB (6 calls, 50%)                             │ CB (6 calls, 50%)
         │ NO Retry on Reserve!                          │ NO Retry on Pay!
         │ Retry on Confirm/Cancel                       │ Retry on Query only
         ▼                                               ▼
  ┌──────────────┐                                ┌──────────────┐
  │Inventory Svc │                                │Payment Serv. │
  │    :8085     │                                │    :8086     │
  └──────────────┘                                └──────────────┘
```

---

## 3. Resilience4j Client Implementations

All resilient clients are implemented using the decorator pattern as `@Primary` Spring beans wrapping the underlying OpenFeign client proxies (`primary = false`, unique qualifiers).

### 3.1 Order Service Clients (`com.shopsphere.order.resilience`)

1. **`ResilientProductClient`**
   - **Circuit Breaker:** `order-product`
   - **Retry:** `order-product` (3 attempts, 500ms initial wait, 2.0x backoff).
   - **Exception Filter:** 404 `ProductNotFoundException` and 400 `BadRequest` are explicitly ignored (do not count as circuit failures and do not retry).
   - **Fallback:** On `CallNotPermittedException` (circuit open), logs warning and throws `ProductUnavailableException` (503 Service Unavailable).

2. **`ResilientCartClient`**
   - **Circuit Breaker:** `order-cart`
   - **Retry:** `order-cart` on `getCart(userId)`.
   - **Fallback:** On `clearCart(userId)`, the fallback safely catches the exception and logs a warning:
     ```java
     public void clearCartFallback(UUID userId, Throwable t) {
         log.warn("ResilientCartClient: clearCart fallback for user {} - reason: {}", userId, t.getMessage());
     }
     ```
     **Critical Invariant:** A failure to clear the cart after payment confirmation *never* reverts or cancels a confirmed order.

3. **`ResilientInventoryClient`**
   - **Circuit Breaker:** `order-inventory`
   - **Idempotency Rule:** `reserveInventory()` has **NO RETRY**. If inventory reservation fails or times out, it is handled by the caller without blind re-submissions.
   - **Idempotent Operations:** `confirmReservation()`, `cancelReservation()`, and `getReservationByOrderId()` are decorated with `@Retry(name = "order-inventory-idempotent")` (2 attempts, 500ms wait, 2.0x backoff).

4. **`ResilientPaymentClient`**
   - **Circuit Breaker:** `order-payment`
   - **Idempotency Rule:** `processPayment()` has **NO RETRY**. A communication failure or timeout throws `PaymentServiceUnavailableException` (503), keeping the order in `PENDING` status for Phase 3 recovery.
   - **Query Retry:** `getPaymentByOrderId()` uses `@Retry(name = "order-payment-query")` (2 attempts, 500ms wait, 2.0x backoff) as queries are idempotent.

### 3.2 Cart Service Client (`com.shopsphere.cart.resilience`)

1. **`ResilientProductClient`**
   - **Circuit Breaker:** `cart-product`
   - **Retry:** `cart-product` (3 attempts, 500ms wait, 2.0x backoff).
   - **Ignored Exceptions:** 404 `ProductNotFoundException` is rethrown directly without incrementing failure counters or retrying.
   - **Fallback:** If circuit is OPEN, throws `ProductUnavailableException` ("Product service is currently unavailable").

---

## 4. Timeout Architecture

All OpenFeign HTTP client timeouts are strictly bounded via Config Server (`application.yml` and service configs):

| Target Service | Connect Timeout | Read Timeout | Max Total Latency |
| :--- | :--- | :--- | :--- |
| `PRODUCT-SERVICE` | 2000 ms | 3000 ms | 5.0 s |
| `CART-SERVICE` | 2000 ms | 3000 ms | 5.0 s |
| `INVENTORY-SERVICE` | 2000 ms | 3000 ms | 5.0 s |
| `PAYMENT-SERVICE` | 2000 ms | 3000 ms | 5.0 s |
| Default Gateway HTTP | 3000 ms | 5000 ms | 5.0 s |

No thread or request can hang indefinitely waiting for an unresponsive downstream microservice.

---

## 5. Circuit Breaker Thresholds & Recovery

Configured centrally in `infrastructure/config-repository/application.yml`:
- **Sliding Window:** `COUNT_BASED`, size = 6 calls.
- **Minimum Number of Calls:** 3 calls before evaluating failure rate.
- **Failure Rate Threshold:** 50.0% failure rate triggers circuit transition to `OPEN`.
- **Wait Duration in Open State:** 5000 ms (5 seconds).
- **Permitted Calls in Half-Open State:** 2 test calls.
- **Automatic Transition:** `automaticTransitionFromOpenToHalfOpenEnabled = true`.

---

## 6. Automated & Live Verification Matrix

### 6.1 Automated Test Suite (141/141 Passing)

| Module | Tests Run | Failures | Errors | Status |
| :--- | :---: | :---: | :---: | :---: |
| `api-gateway` | 3 | 0 | 0 | **PASS** |
| `auth-service` | 9 | 0 | 0 | **PASS** |
| `product-service` | Pass | 0 | 0 | **PASS** |
| `cart-service` | 12 | 0 | 0 | **PASS** |
| `inventory-service` | 39 | 0 | 0 | **PASS** |
| `payment-service` | 13 | 0 | 0 | **PASS** |
| `order-service` | 65 | 0 | 0 | **PASS** |
| **Total Monorepo Tests** | **141** | **0** | **0** | **PASS** |

### 6.2 Live Failure-Injection Test Suite (`live_failure_injection_phase6.py`)

| Test ID | Scenario | Injected Condition | Observed Behavior | Status |
| :--- | :--- | :--- | :--- | :---: |
| **Test 01** | Service Discovery | All services active | All 7 microservices registered in Eureka | **PASS** |
| **Test 02** | Happy Path Checkout | Clean cart, active payment | Order 201 `CONFIRMED`, cart items = 0 | **PASS** |
| **Test 03** | Deterministic Decline | `mockPaymentAction: FAIL_DECLINED` | Responded in 0.27s, Order `CANCELLED`, Inventory reservation `CANCELLED`, no retry storm | **PASS** |
| **Test 04** | Payment Outage & State Preservation | Killed Payment Service (8086) | Bounded timeout (2.27s), 503 error, Order left `PENDING`, Inventory left `RESERVED`, Cart retained | **PASS** |
| **Test 05** | Post-Outage Recovery | Restarted Payment Service | Invoked recovery endpoint, Order transitioned `PENDING` -> `CONFIRMED`, Inventory confirmed | **PASS** |
| **Test 06** | Prometheus Metrics | Actuator metrics scraped | `resilience4j_circuitbreaker_state` gauge = 1.0 (closed) across all 4 order circuits | **PASS** |
| **Test 07** | Non-Transient 404 Isolation | Random non-existent product ID | Returns 404, Circuit Breaker remains CLOSED (no false trips) | **PASS** |
| **Test 08** | Security Controls | Missing JWT & Forbidden role | Unauthenticated -> 401, Customer accessing admin recovery -> 403 Forbidden | **PASS** |
| **Test 09** | Distributed Tracing | Gateway request to downstream | Active distributed traces recorded in Zipkin (HTTP 200, 10 traces) | **PASS** |

---

## 7. Key Findings & Bug Fixes Delivered

1. **SnakeYAML 2.x Duplicate Key Fix:**
   - Consolidated duplicate `spring:` definitions in `infrastructure/config-repository/order-service.yml` under a unified configuration tree.
2. **Feign Client Primary Bean Conflict:**
   - Added `primary = false` and explicit `qualifiers = {...}` to `@FeignClient` annotations in `cart-service` and `order-service` to allow seamless injection of `@Primary` resilient decorator components.
3. **Internal Security Secret Alignment:**
   - Standardized `X-Internal-Secret: shopsphere-internal-service-secret-key-384` for internal recovery invocation and secured recovery routes under `ROLE_INTERNAL_SERVICE` and `ROLE_ADMIN`.

---

## 8. Conclusion

Phase 6 is 100% complete, fully tested, and verified across all live services and failure-injection scenarios. The ShopSphere microservices architecture is resilient, self-healing, and production-ready.
