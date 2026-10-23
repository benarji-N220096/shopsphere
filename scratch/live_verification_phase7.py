import requests
import time
import json
import uuid
import sys
import re

print("==========================================================")
print("  SHOPSPHERE PHASE 7 — LIVE OBSERVABILITY VERIFICATION    ")
print("==========================================================")

GATEWAY_URL = "http://localhost:8080"
ZIPKIN_URL = "http://localhost:9411"
INTERNAL_SECRET = "shopsphere-internal-service-secret-key-384"

SERVICES = {
    "config-server": "http://localhost:8888",
    "discovery-server": "http://localhost:8761",
    "api-gateway": "http://localhost:8080",
    "auth-service": "http://localhost:8081",
    "product-service": "http://localhost:8082",
    "cart-service": "http://localhost:8083",
    "order-service": "http://localhost:8084",
    "inventory-service": "http://localhost:8085",
    "payment-service": "http://localhost:8086"
}

results = {
    "health_checks": {},
    "actuator_prometheus": {},
    "business_metrics": {},
    "resilience_metrics": {},
    "distributed_tracing": {},
    "recovery_metrics": {},
    "security_verification": {},
    "leak_audit": {}
}

# ----------------------------------------------------
# 1. WAIT FOR SERVICES TO BE HEALTHY
# ----------------------------------------------------
print("\n[STEP 1] Checking /actuator/health for all services...")

for name, base_url in SERVICES.items():
    healthy = False
    endpoint = f"{base_url}/actuator/health"
    print(f"Waiting for {name} ({endpoint})...")
    start_time = time.time()
    while time.time() - start_time < 30:
        try:
            r = requests.get(endpoint, timeout=3)
            if r.status_code == 200:
                data = r.json()
                if data.get("status") in ["UP", "DEGRADED"]:
                    healthy = True
                    results["health_checks"][name] = {"status": "UP", "details": data}
                    print(f"  -> {name} is UP!")
                    break
        except Exception:
            pass
        time.sleep(2)
    if not healthy:
        print(f"  -> WARNING: {name} not responding UP on {endpoint}")
        results["health_checks"][name] = {"status": "DOWN"}

# Also check Zipkin
try:
    zr = requests.get(f"{ZIPKIN_URL}/health", timeout=3)
    if zr.status_code == 200:
        print("  -> Zipkin is UP on port 9411!")
        results["health_checks"]["zipkin"] = {"status": "UP"}
    else:
        results["health_checks"]["zipkin"] = {"status": f"HTTP {zr.status_code}"}
except Exception as e:
    results["health_checks"]["zipkin"] = {"status": str(e)}

# ----------------------------------------------------
# 2. VERIFY PROMETHEUS SCRAPING / METRICS EXPOSURE
# ----------------------------------------------------
print("\n[STEP 2] Verifying /actuator/prometheus endpoints...")
for name in ["api-gateway", "auth-service", "product-service", "cart-service", "order-service", "inventory-service", "payment-service"]:
    base_url = SERVICES[name]
    try:
        r = requests.get(f"{base_url}/actuator/prometheus", timeout=5)
        if r.status_code == 200 and "jvm_memory_used_bytes" in r.text:
            has_hikari = "hikaricp" in r.text or "hikaricp" in r.text.lower()
            results["actuator_prometheus"][name] = {
                "status": "PASS",
                "bytes_scraped": len(r.text),
                "has_jvm": True,
                "has_hikari": has_hikari
            }
            print(f"  -> {name}: Prometheus metrics active (size: {len(r.text)} bytes, hikari: {has_hikari})")
        else:
            results["actuator_prometheus"][name] = {"status": "FAIL", "http_status": r.status_code}
            print(f"  -> {name}: Prometheus endpoint returned {r.status_code}")
    except Exception as ex:
        results["actuator_prometheus"][name] = {"status": "ERROR", "error": str(ex)}
        print(f"  -> {name}: Error scraping prometheus: {ex}")

# ----------------------------------------------------
# 3. LIVE E-COMMERCE WORKFLOW & DOMAIN METRICS
# ----------------------------------------------------
print("\n[STEP 3] Executing End-to-End Live Workflow via API Gateway & Verifying Domain Metrics...")

# Helper to query prometheus metric on service
def get_metric_value(service_name, metric_prefix):
    base_url = SERVICES[service_name]
    try:
        r = requests.get(f"{base_url}/actuator/prometheus", timeout=3)
        if r.status_code == 200:
            for line in r.text.splitlines():
                if line.startswith(metric_prefix) and not line.startswith(f"{metric_prefix}_"):
                    parts = line.split()
                    if len(parts) >= 2:
                        return float(parts[1])
                elif line.startswith(metric_prefix):
                    parts = line.split()
                    if len(parts) >= 2:
                        try:
                            return float(parts[1])
                        except ValueError:
                            pass
    except Exception:
        pass
    return None

# Register and login user
test_user = f"obs_user_{uuid.uuid4().hex[:6]}"
test_email = f"{test_user}@testshopsphere.com"
test_pass = "ComplexPass@123"

print(f"Registering user {test_user} via Gateway...")
reg_payload = {
    "username": test_user,
    "email": test_email,
    "password": test_pass,
    "firstName": "Observability",
    "lastName": "Tester"
}
reg_resp = requests.post(f"{GATEWAY_URL}/api/v1/auth/register", json=reg_payload, timeout=5)
print(f"Registration response: {reg_resp.status_code}")

login_payload = {"email": test_email, "password": test_pass}
login_resp = requests.post(f"{GATEWAY_URL}/api/v1/auth/login", json=login_payload, timeout=5)
token = None
user_id = None
if login_resp.status_code == 200:
    token_data = login_resp.json()
    token = token_data.get("token") or token_data.get("accessToken")
    user_id = token_data.get("userId") or token_data.get("id")
    print(f"Login successful! User ID: {user_id}")
else:
    print(f"Login failed: {login_resp.status_code} - {login_resp.text}")

auth_headers = {"Authorization": f"Bearer {token}"} if token else {}

# Verify Trace Correlation on Gateway Response
gateway_trace_id = login_resp.headers.get("X-Trace-Id")
gateway_req_id = login_resp.headers.get("X-Request-Id")
print(f"Gateway Response Headers: X-Trace-Id={gateway_trace_id}, X-Request-Id={gateway_req_id}")
results["distributed_tracing"]["gateway_correlation_headers"] = {
    "X-Trace-Id": gateway_trace_id,
    "X-Request-Id": gateway_req_id
}

# Fetch Products
prod_resp = requests.get(f"{GATEWAY_URL}/api/v1/products", headers=auth_headers, timeout=5)
product_id = None
product_price = 49.99
if prod_resp.status_code == 200:
    products = prod_resp.json()
    if isinstance(products, list) and len(products) > 0:
        product_id = products[0].get("id")
        product_price = float(products[0].get("price", 49.99))
        print(f"Using Product: {product_id} (Price: {product_price})")
    elif isinstance(products, dict) and "content" in products and len(products["content"]) > 0:
        product_id = products["content"][0].get("id")
        product_price = float(products["content"][0].get("price", 49.99))
        print(f"Using Product: {product_id} (Price: {product_price})")

# Add to Cart
if product_id:
    cart_add_payload = {
        "productId": product_id,
        "quantity": 2
    }
    cart_resp = requests.post(f"{GATEWAY_URL}/api/v1/cart/items", json=cart_add_payload, headers=auth_headers, timeout=5)
    print(f"Add to Cart response: {cart_resp.status_code}")
    
    # Check Cart Prometheus metrics
    cart_added_metric = get_metric_value("cart-service", "cart_items_added_total")
    print(f"cart_items_added_total metric: {cart_added_metric}")
    results["business_metrics"]["cart_items_added_total"] = cart_added_metric

# Place Order
order_id = None
if product_id and token:
    order_payload = {
        "items": [
            {
                "productId": product_id,
                "quantity": 1,
                "price": product_price
            }
        ],
        "shippingAddress": "123 Metric Way, Observability City, OC 12345",
        "paymentMethod": "CREDIT_CARD"
    }
    order_resp = requests.post(f"{GATEWAY_URL}/api/v1/orders", json=order_payload, headers=auth_headers, timeout=10)
    print(f"Place Order response: {order_resp.status_code}")
    order_trace_id = order_resp.headers.get("X-Trace-Id")
    print(f"Order Response X-Trace-Id: {order_trace_id}")
    
    if order_resp.status_code in [200, 201]:
        order_data = order_resp.json()
        order_id = order_data.get("id") or order_data.get("orderId")
        print(f"Order created successfully! ID: {order_id}, Status: {order_data.get('status')}")
    else:
        print(f"Order placement failed: {order_resp.text}")

    # Allow metrics and traces to flush
    time.sleep(2)

    # Check Domain Metrics
    results["business_metrics"]["orders_created_total"] = (
        get_metric_value("order-service", "orders_total") or
        get_metric_value("order-service", "orders_created_total")
    )
    results["business_metrics"]["orders_confirmed_total"] = get_metric_value("order-service", "orders_confirmed_total")
    results["business_metrics"]["orders_duration_seconds_count"] = get_metric_value("order-service", "orders_duration_seconds_count")
    results["business_metrics"]["payments_processed_total"] = get_metric_value("payment-service", "payments_processed_total")
    results["business_metrics"]["payments_duration_seconds_count"] = get_metric_value("payment-service", "payments_duration_seconds_count")
    results["business_metrics"]["inventory_reservations_created_total"] = (
        get_metric_value("inventory-service", "inventory_reservations_total") or
        get_metric_value("inventory-service", "inventory_reservations_created_total")
    )
    results["business_metrics"]["inventory_reservations_confirmed_total"] = get_metric_value("inventory-service", "inventory_reservation_confirmed_total")
    results["business_metrics"]["cart_cleared_total"] = get_metric_value("cart-service", "cart_cleared_total")

    print("\nRecorded Happy Path Business Metrics:")
    for k, v in results["business_metrics"].items():
        print(f"  {k} = {v}")

# ----------------------------------------------------
# 3.2 FAILURE INJECTION TEST (FAIL_DECLINED)
# ----------------------------------------------------
print("\n[STEP 3.2] Executing Controlled Failure Injection (mockPaymentAction=FAIL_DECLINED)...")
if product_id and token:
    # Add item to cart
    requests.post(f"{GATEWAY_URL}/api/v1/cart/items", json={"productId": product_id, "quantity": 1}, headers=auth_headers, timeout=5)
    
    # Place order with mock payment decline
    fail_order_payload = {
        "items": [{"productId": product_id, "quantity": 1, "price": product_price}],
        "shippingAddress": "123 Failure Ln, Error City, EC 99999",
        "paymentMethod": "CREDIT_CARD",
        "mockPaymentAction": "FAIL_DECLINED"
    }
    fail_order_resp = requests.post(f"{GATEWAY_URL}/api/v1/orders", json=fail_order_payload, headers=auth_headers, timeout=10)
    print(f"Declined Order response: {fail_order_resp.status_code}")
    print(f"Declined Order Trace ID: {fail_order_resp.headers.get('X-Trace-Id')}")
    
    time.sleep(2)
    cancelled_orders = get_metric_value("order-service", "orders_cancelled_total")
    failed_payments = get_metric_value("payment-service", "payment_failed_total") or get_metric_value("payment-service", "payments_failed_total")
    cancelled_inv = get_metric_value("inventory-service", "inventory_reservation_cancelled_total")
    
    results["failure_injection"] = {
        "status": "PASS" if fail_order_resp.status_code in [400, 422, 500] else "FAIL",
        "http_status": fail_order_resp.status_code,
        "orders_cancelled_total": cancelled_orders,
        "payments_failed_total": failed_payments,
        "inventory_reservation_cancelled_total": cancelled_inv
    }
    print(f"Failure Metrics: cancelled_orders={cancelled_orders}, failed_payments={failed_payments}, cancelled_inv={cancelled_inv}")

# ----------------------------------------------------
# 4. DISTRIBUTED TRACING VERIFICATION VIA ZIPKIN
# ----------------------------------------------------
print("\n[STEP 4] Verifying Distributed Tracing in Zipkin...")
try:
    # Query Zipkin for traces
    zt_resp = requests.get(f"{ZIPKIN_URL}/api/v2/traces?limit=10", timeout=5)
    if zt_resp.status_code == 200:
        traces = zt_resp.json()
        print(f"Found {len(traces)} traces in Zipkin!")
        trace_services = set()
        for tr in traces:
            for span in tr:
                svc = span.get("localEndpoint", {}).get("serviceName")
                if svc:
                    trace_services.add(svc)
        print(f"Services appearing in Zipkin traces: {trace_services}")
        results["distributed_tracing"]["zipkin_traces_found"] = len(traces)
        results["distributed_tracing"]["services_in_traces"] = list(trace_services)
        results["distributed_tracing"]["status"] = "PASS" if len(traces) > 0 else "NO_TRACES"
    else:
        results["distributed_tracing"]["status"] = f"HTTP {zt_resp.status_code}"
except Exception as e:
    results["distributed_tracing"]["status"] = f"ERROR: {e}"

# ----------------------------------------------------
# 5. RESILIENCE4J METRICS VERIFICATION
# ----------------------------------------------------
print("\n[STEP 5] Checking Resilience4j Metrics in order-service / cart-service...")
try:
    r_prom = requests.get(f"{SERVICES['order-service']}/actuator/prometheus", timeout=3)
    if r_prom.status_code == 200:
        cb_state_lines = [l for l in r_prom.text.splitlines() if "resilience4j_circuitbreaker_state" in l and not l.startswith("#")]
        cb_call_lines = [l for l in r_prom.text.splitlines() if "resilience4j_circuitbreaker_calls_seconds_count" in l and not l.startswith("#")]
        retry_lines = [l for l in r_prom.text.splitlines() if "resilience4j_retry_calls_total" in l and not l.startswith("#")]
        
        results["resilience_metrics"]["circuitbreaker_instances"] = len(cb_state_lines)
        results["resilience_metrics"]["circuitbreaker_state_samples"] = cb_state_lines[:5]
        results["resilience_metrics"]["has_retry_metrics"] = len(retry_lines) > 0
        print(f"Found {len(cb_state_lines)} circuit breaker state metrics!")
        print(f"Found {len(cb_call_lines)} circuit breaker call metrics!")
        print(f"Found {len(retry_lines)} retry metrics!")
except Exception as e:
    results["resilience_metrics"]["error"] = str(e)

# ----------------------------------------------------
# 6. PHASE 3 RECOVERY OBSERVABILITY
# ----------------------------------------------------
print("\n[STEP 6] Triggering Batch Order Recovery & Verifying Telemetry...")
try:
    rec_headers = {"X-Internal-Secret": INTERNAL_SECRET}
    rec_resp = requests.post(f"{SERVICES['order-service']}/api/v1/orders/recover-pending", headers=rec_headers, timeout=5)
    print(f"Order Recovery endpoint response: {rec_resp.status_code} - {rec_resp.text}")
    
    time.sleep(1)
    rec_cycles = get_metric_value("order-service", "orders_recovery_cycles_total")
    rec_dur = get_metric_value("order-service", "orders_recovery_duration_seconds_count")
    results["recovery_metrics"]["orders_recovery_cycles_total"] = rec_cycles
    results["recovery_metrics"]["orders_recovery_duration_seconds_count"] = rec_dur
    results["recovery_metrics"]["status"] = "PASS" if rec_cycles is not None and rec_cycles > 0 else "FAIL"
    print(f"Recovery Metrics: cycles={rec_cycles}, duration_count={rec_dur}")
except Exception as e:
    results["recovery_metrics"]["error"] = str(e)

# ----------------------------------------------------
# 7. SECURITY & PERIMETER VERIFICATION
# ----------------------------------------------------
print("\n[STEP 7] Verifying Gateway Perimeter Security & Header Stripping...")

# 7.1 Internal Secret Stripping at Gateway
gw_sec_resp = requests.post(
    f"{GATEWAY_URL}/api/v1/orders/recover-pending",
    headers={"X-Internal-Secret": INTERNAL_SECRET},
    timeout=5
)
print(f"Gateway access to /api/v1/orders/recover-pending: {gw_sec_resp.status_code}")
results["security_verification"]["gateway_blocks_or_strips_internal_secret"] = (
    gw_sec_resp.status_code in [401, 403, 404]
)

# 7.2 Internal inventory endpoint blocked at Gateway
gw_inv_resp = requests.post(
    f"{GATEWAY_URL}/api/v1/inventory/reservations",
    json={"orderId": str(uuid.uuid4()), "items": []},
    headers={"X-Internal-Secret": INTERNAL_SECRET},
    timeout=5
)
print(f"Gateway access to /api/v1/inventory/reservations: {gw_inv_resp.status_code}")
results["security_verification"]["gateway_blocks_internal_inventory"] = (
    gw_inv_resp.status_code in [401, 403, 404]
)

# ----------------------------------------------------
# 8. SENSITIVE LEAK & REDACTION AUDIT
# ----------------------------------------------------
print("\n[STEP 8] Auditing Prometheus & Actuator Endpoints for Sensitive Information Leakage...")
sensitive_patterns = [
    "shopsphere-internal-service-secret",
    "ComplexPass@123",
    "password=",
    "cvv",
    "Bearer eyJ"
]

leaks_found = []
for name in ["api-gateway", "auth-service", "product-service", "cart-service", "order-service", "inventory-service", "payment-service"]:
    base_url = SERVICES[name]
    try:
        r = requests.get(f"{base_url}/actuator/prometheus", timeout=3)
        if r.status_code == 200:
            for pattern in sensitive_patterns:
                if pattern in r.text:
                    leaks_found.append(f"{name}/actuator/prometheus leaked '{pattern}'")
    except Exception:
        pass

if not leaks_found:
    print("  -> ZERO sensitive data leaks found across all Prometheus endpoints! Clean!")
    results["leak_audit"]["status"] = "CLEAN"
else:
    print(f"  -> WARNING: Leaks detected: {leaks_found}")
    results["leak_audit"]["status"] = "LEAKS_DETECTED"
    results["leak_audit"]["leaks"] = leaks_found

# ----------------------------------------------------
# SUMMARY
# ----------------------------------------------------
print("\n==========================================================")
print("  PHASE 7 LIVE VERIFICATION SUMMARY")
print("==========================================================")
print(json.dumps(results, indent=2))

with open("scratch/phase7_live_results.json", "w") as f:
    json.dump(results, f, indent=2)

print("\nResults saved to scratch/phase7_live_results.json")
