import urllib.request
import urllib.error
import json
import time

GATEWAY_URL = 'http://localhost:8080'
FRONTEND_URL = 'http://localhost:5173'

def post_json(endpoint, data, token=None):
    url = f"{GATEWAY_URL}{endpoint}"
    payload = json.dumps(data).encode('utf-8')
    headers = {'Content-Type': 'application/json', 'Accept': 'application/json'}
    if token:
        headers['Authorization'] = f'Bearer {token}'
    req = urllib.request.Request(url, data=payload, headers=headers, method='POST')
    try:
        with urllib.request.urlopen(req) as resp:
            content = resp.read().decode('utf-8')
            return resp.status, json.loads(content) if content else None
    except urllib.error.HTTPError as e:
        content = e.read().decode('utf-8')
        try:
            return e.code, json.loads(content) if content else None
        except:
            return e.code, content

def put_json(endpoint, data, token=None):
    url = f"{GATEWAY_URL}{endpoint}"
    payload = json.dumps(data).encode('utf-8')
    headers = {'Content-Type': 'application/json', 'Accept': 'application/json'}
    if token:
        headers['Authorization'] = f'Bearer {token}'
    req = urllib.request.Request(url, data=payload, headers=headers, method='PUT')
    try:
        with urllib.request.urlopen(req) as resp:
            content = resp.read().decode('utf-8')
            return resp.status, json.loads(content) if content else None
    except urllib.error.HTTPError as e:
        content = e.read().decode('utf-8')
        try:
            return e.code, json.loads(content) if content else None
        except:
            return e.code, content

def get_json(endpoint, token=None):
    url = f"{GATEWAY_URL}{endpoint}"
    headers = {'Accept': 'application/json'}
    if token:
        headers['Authorization'] = f'Bearer {token}'
    req = urllib.request.Request(url, headers=headers, method='GET')
    try:
        with urllib.request.urlopen(req) as resp:
            content = resp.read().decode('utf-8')
            return resp.status, json.loads(content) if content else None
    except urllib.error.HTTPError as e:
        content = e.read().decode('utf-8')
        try:
            return e.code, json.loads(content) if content else None
        except:
            return e.code, content

def delete_req(endpoint, token=None):
    url = f"{GATEWAY_URL}{endpoint}"
    headers = {}
    if token:
        headers['Authorization'] = f'Bearer {token}'
    req = urllib.request.Request(url, headers=headers, method='DELETE')
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, None
    except urllib.error.HTTPError as e:
        return e.code, None

def check_frontend_route(path):
    url = f"{FRONTEND_URL}{path}"
    req = urllib.request.Request(url, headers={'Accept': 'text/html'})
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status
    except Exception as e:
        return None

def run_tests():
    print("==================================================")
    print("SHOPSPHERE PHASE 10 — FULL SYSTEM VERIFICATION")
    print("==================================================")

    # 1. Frontend server health & routes
    print("\n--- 1. Testing Frontend SPA Routes Served ---")
    routes = ['/', '/products', '/cart', '/login', '/admin', '/admin/products', '/admin/inventory', '/admin/orders', '/403', '/500']
    for r in routes:
        st = check_frontend_route(r)
        assert st == 200, f"Route {r} returned status {st}"
        print(f"  [PASS] Route {r} -> HTTP 200")

    # 2. Authentication: Login as Admin
    print("\n--- 2. Testing Admin Authentication & RBAC JWT ---")
    admin_login_status, admin_auth = post_json('/api/v1/auth/login', {
        'email': 'admin@shopsphere.com',
        'password': 'Admin@12345'
    })
    assert admin_login_status == 200, f"Admin login failed: {admin_login_status}"
    admin_token = admin_auth['accessToken']
    roles = admin_auth.get('roles', [])
    assert 'ADMIN' in roles or 'ROLE_ADMIN' in roles, f"Admin role missing: {roles}"
    print(f"  [PASS] Admin authenticated successfully. Roles: {roles}")

    # 3. Customer Authentication
    print("\n--- 3. Testing Customer Authentication & Security Boundary ---")
    cust_email = f"phase10_cust_{int(time.time())}@example.com"
    reg_st, _ = post_json('/api/v1/auth/register', {
        'email': cust_email,
        'password': 'Password@123',
        'firstName': 'Phase10',
        'lastName': 'Customer'
    })
    assert reg_st == 200 or reg_st == 201, f"Customer registration failed: {reg_st}"
    cust_st, cust_auth = post_json('/api/v1/auth/login', {
        'email': cust_email,
        'password': 'Password@123'
    })
    cust_token = cust_auth['accessToken']
    cust_roles = cust_auth.get('roles', [])
    assert 'ADMIN' not in cust_roles and 'ROLE_ADMIN' not in cust_roles, "Customer has admin role!"
    print(f"  [PASS] Customer authenticated successfully. Roles: {cust_roles}")

    # 4. Security Check: Customer blocked from Admin operations (RBAC)
    print("\n--- 4. Testing Authorization Enforcement (403 for Customer) ---")
    blocked_create_st, _ = post_json('/api/v1/products', {
        'name': 'Unauthorized Product',
        'description': 'Customer should never be allowed to create this',
        'price': 10.0,
        'stockQuantity': 5,
        'category': 'Electronics',
        'active': True
    }, token=cust_token)
    assert blocked_create_st == 403, f"Expected 403 Forbidden for customer, got {blocked_create_st}"
    print("  [PASS] Customer attempt to create product correctly blocked -> HTTP 403")

    # 5. Admin Dashboard Metrics & Data
    print("\n--- 5. Testing Admin Dashboard Aggregates ---")
    st, prods = get_json('/api/v1/products', token=admin_token)
    assert st == 200 and isinstance(prods, list) and len(prods) > 0, "Failed to fetch products"
    print(f"  [PASS] Admin fetched {len(prods)} products for catalog telemetry.")

    st, orders = get_json('/api/v1/orders', token=admin_token)
    assert st == 200 and isinstance(orders, list), "Failed to fetch platform orders"
    print(f"  [PASS] Admin fetched {len(orders)} orders across entire platform.")

    first_prod_id = prods[0]['id']
    st, inv = get_json(f'/api/v1/inventory/{first_prod_id}', token=admin_token)
    assert st == 200 and 'availableQuantity' in inv, "Failed to fetch inventory"
    print(f"  [PASS] Admin fetched inventory telemetry: Total={inv['quantity']}, Reserved={inv['reservedQuantity']}, Available={inv['availableQuantity']}")

    # 6. Admin Product Management (Full CRUD)
    print("\n--- 6. Testing Admin Product CRUD Operations ---")
    test_prod_name = f"Test Pro Item {int(time.time())}"
    create_st, new_prod = post_json('/api/v1/products', {
        'name': test_prod_name,
        'description': 'Created during Phase 10 verification test',
        'price': 89.99,
        'stockQuantity': 42,
        'category': 'Electronics',
        'imageUrl': 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80',
        'active': True
    }, token=admin_token)
    assert create_st == 200 or create_st == 201, f"Failed to create product: {create_st}"
    new_prod_id = new_prod['id']
    print(f"  [PASS] Product created successfully: {new_prod_id} ({new_prod['name']}) -> HTTP {create_st}")

    # Update product
    update_st, updated_prod = put_json(f'/api/v1/products/{new_prod_id}', {
        'name': f"{test_prod_name} [UPDATED]",
        'description': 'Updated description',
        'price': 99.99,
        'stockQuantity': 55,
        'category': 'Electronics',
        'imageUrl': new_prod['imageUrl'],
        'active': True
    }, token=admin_token)
    assert update_st == 200, f"Failed to update product: {update_st}"
    assert updated_prod['price'] == 99.99, "Price update not reflected"
    print(f"  [PASS] Product updated successfully: Price updated to ${updated_prod['price']} -> HTTP 200")

    # Delete product
    del_st, _ = delete_req(f'/api/v1/products/{new_prod_id}', token=admin_token)
    assert del_st == 200 or del_st == 204, f"Failed to delete product: {del_st}"
    print(f"  [PASS] Product deleted successfully -> HTTP {del_st}")

    # 7. Admin Inventory Stock Update
    print("\n--- 7. Testing Admin Inventory Stock Adjustment ---")
    inv_update_st, updated_inv = put_json(f'/api/v1/inventory/{first_prod_id}', {
        'quantity': 75
    }, token=admin_token)
    assert inv_update_st == 200, f"Failed to update inventory: {inv_update_st}"
    assert updated_inv['quantity'] == 75, f"Quantity expected 75, got {updated_inv['quantity']}"
    print(f"  [PASS] Inventory stock adjusted to {updated_inv['quantity']} units -> HTTP 200")

    # 8. Admin Recovery Visibility & Gateway Protection
    print("\n--- 8. Testing Admin Recovery Visibility & Gateway Boundary Protection ---")
    rec_st, _ = post_json('/api/v1/orders/recover-pending', {}, token=admin_token)
    assert rec_st == 403, f"Gateway should deny public invocation of internal recovery, got: {rec_st}"
    print(f"  [PASS] Gateway correctly denies public/browser invocation of internal recovery -> HTTP 403")

    # 9. Phase 8 & 9 Customer Storefront Regression Testing
    print("\n--- 9. Testing Zero Regressions to Customer Storefront Flows ---")
    # Customer cart
    add_cart_st, cart = post_json('/api/v1/cart/items', {
        'productId': first_prod_id,
        'quantity': 1
    }, token=cust_token)
    assert add_cart_st == 200 or add_cart_st == 201, f"Failed to add item to customer cart: {add_cart_st}"
    print(f"  [PASS] Customer added product to cart -> HTTP {add_cart_st}")

    # Customer checkout
    order_st, placed_order = post_json('/api/v1/orders', {
        'paymentMethod': 'CREDIT_CARD',
        'mockPaymentAction': 'SUCCESS'
    }, token=cust_token)
    assert order_st == 201, f"Failed to place customer order: {order_st}"
    cust_order_id = placed_order['id']
    print(f"  [PASS] Customer placed order #{cust_order_id} ({placed_order['status']}) -> HTTP 201")

    # Customer query their orders
    my_orders_st, my_orders = get_json('/api/v1/orders', token=cust_token)
    assert my_orders_st == 200 and len(my_orders) >= 1, "Failed to get customer orders"
    assert any(o['id'] == cust_order_id for o in my_orders), "Placed order not in customer order list"
    print(f"  [PASS] Customer fetched their orders (isolated from other users) -> HTTP 200")

    # Admin inspect the specific customer order
    admin_inspect_st, inspected_order = get_json(f'/api/v1/orders/{cust_order_id}', token=admin_token)
    assert admin_inspect_st == 200 and inspected_order['id'] == cust_order_id, "Admin failed to inspect customer order"
    print(f"  [PASS] Admin inspected customer order #{cust_order_id} successfully -> HTTP 200")

    print("\n==================================================")
    print("ALL 9 PHASE 10 VERIFICATION CATEGORIES PASSED!")
    print("==================================================")

if __name__ == '__main__':
    run_tests()
