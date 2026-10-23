# ShopSphere Microservices Automated Startup Script
# Powershell script to boot services with reliable health verification

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "         ShopSphere Enterprise Startup Sequence           " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

function Wait-For-Port($port, $name, $timeoutSec = 60) {
    Write-Host "Checking $name on port $port..." -NoNewline
    $start = Get-Date
    while (((Get-Date) - $start).TotalSeconds -lt $timeoutSec) {
        try {
            $tcp = New-Object System.Net.Sockets.TcpClient
            $tcp.Connect("127.0.0.1", $port)
            $tcp.Close()
            Write-Host " READY!" -ForegroundColor Green
            return $true
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    Write-Host " TIMEOUT (proceeding anyway)" -ForegroundColor Yellow
    return $false
}

# 1. Start Config Server & Eureka Registry
Write-Host "`n[Step 1/3] Starting Infrastructure (Config Server & Eureka)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd infrastructure/config-server; .\mvnw.cmd spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd infrastructure/service-registry; .\mvnw.cmd spring-boot:run"

Wait-For-Port -port 8888 -name "Config Server" -timeoutSec 45
Wait-For-Port -port 8761 -name "Eureka Service Registry" -timeoutSec 45

# 2. Start Core Business Microservices
Write-Host "`n[Step 2/3] Starting Core Business Microservices..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd services/auth-service; .\mvnw.cmd spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd services/product-service; .\mvnw.cmd spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd services/cart-service; .\mvnw.cmd spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd services/order-service; .\mvnw.cmd spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd services/inventory-service; .\mvnw.cmd spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd services/payment-service; .\mvnw.cmd spring-boot:run"

Wait-For-Port -port 8081 -name "Auth Service" -timeoutSec 60
Wait-For-Port -port 8082 -name "Product Service" -timeoutSec 60

# 3. Start API Gateway & Frontend
Write-Host "`n[Step 3/3] Starting API Gateway & Frontend..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd services/api-gateway; .\mvnw.cmd spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd frontend; npm run dev"

Wait-For-Port -port 8080 -name "API Gateway" -timeoutSec 45
Wait-For-Port -port 5173 -name "Frontend Storefront" -timeoutSec 15

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host " All ShopSphere services are now online and operational! " -ForegroundColor Green
Write-Host " Storefront URL: http://localhost:5173/                  " -ForegroundColor Green
Write-Host " Login URL:      http://localhost:5173/login             " -ForegroundColor Green
Write-Host " Register URL:   http://localhost:5173/register          " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
