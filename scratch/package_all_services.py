import os
import subprocess
import sys

services = [
    'infrastructure/config-server',
    'infrastructure/service-registry',
    'services/api-gateway',
    'services/auth-service',
    'services/product-service',
    'services/cart-service',
    'services/inventory-service',
    'services/payment-service',
    'services/order-service'
]

print("Starting package build for all 9 Spring Boot services...")
all_passed = True
for s in services:
    cwd = os.path.join(os.getcwd(), s.replace('/', os.sep))
    mvn = os.path.join(cwd, 'mvnw.cmd')
    p = subprocess.run([mvn, 'package', '-DskipTests'], cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    passed = 'BUILD SUCCESS' in p.stdout
    status = "BUILD SUCCESS" if passed else "BUILD FAIL"
    print(f"[{status}] {s}")
    if not passed:
        print(p.stdout[-1500:])
        all_passed = False

if not all_passed:
    sys.exit(1)
print("\n>>> ALL 9 SPRING BOOT SERVICES PACKAGED SUCCESSFULLY! <<<")
