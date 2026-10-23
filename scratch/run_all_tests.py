import subprocess
import os
import sys

services = [
    'api-gateway',
    'auth-service',
    'product-service',
    'cart-service',
    'inventory-service',
    'payment-service',
    'order-service'
]

results = {}

for s in services:
    cwd = os.path.join(os.getcwd(), 'services', s)
    print(f"=== Testing {s} ===")
    mvn = os.path.join(cwd, 'mvnw.cmd')
    p = subprocess.run([mvn, 'test'], cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    passed = 'BUILD SUCCESS' in p.stdout
    
    # Extract test counts from output
    # Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
    test_summary = "N/A"
    for line in p.stdout.splitlines():
        if "Tests run:" in line and "Failures:" in line:
            test_summary = line.strip()
    
    results[s] = (passed, test_summary)
    print(f"{s}: {'PASS' if passed else 'FAIL'} ({test_summary})")
    if not passed:
        print("\n--- FAILURE LOG ---")
        print("\n".join(p.stdout.splitlines()[-30:]))

print("\n==============================================")
print("TEST EXECUTION SUMMARY")
print("==============================================")
all_ok = True
for s, (passed, summary) in results.items():
    status = "BUILD SUCCESS" if passed else "BUILD FAILURE"
    print(f"{s:20} : {status:15} | {summary}")
    if not passed:
        all_ok = False

if not all_ok:
    sys.exit(1)
else:
    print("\nALL 7 SERVICES PASSED ALL TESTS SUCCESSFULLY!")
