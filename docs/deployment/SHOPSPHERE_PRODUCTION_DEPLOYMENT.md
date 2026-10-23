# ShopSphere — Production Deployment & Operations Guide
## Complete Cloud Setup, TLS/HTTPS, CI/CD, and Disaster Recovery

---

## 1. Overview & Cloud Prerequisites

This guide outlines the step-by-step procedure to provision, configure, deploy, and maintain the ShopSphere microservices platform in a production cloud environment.

### Target Cloud Specification:
- **Cloud Provider**: AWS (Amazon Web Services), DigitalOcean, Hetzner, or any standard Linux cloud host.
- **Virtual Machine Specification**:
  - **Operating System**: Ubuntu 24.04 LTS (x86_64)
  - **vCPU**: 4 vCPUs
  - **Memory (RAM)**: 8 GB minimum (16 GB recommended, e.g., AWS `t3.xlarge` or Hetzner `CPX41`)
  - **Storage**: 50–60 GB gp3 SSD (encrypted)
- **Networking**: Public Static IPv4 (Elastic IP) with Ports 80, 443, and 22 enabled in the firewall.

---

## 2. Infrastructure Provisioning via Terraform (Automated)

If deploying to AWS, use the pre-configured Terraform modules in `infrastructure/terraform/`:

1. **Navigate to Terraform directory**:
   ```bash
   cd infrastructure/terraform
   ```
2. **Configure Variables**:
   Copy the example template and insert your key pair name and domain:
   ```bash
   cp terraform.tfvars.example terraform.tfvars
   # Edit terraform.tfvars:
   # key_name       = "your-aws-ssh-key"
   # admin_ssh_cidr = "<your-ip>/32"
   # domain_name    = "shopsphere.yourdomain.com"
   ```
3. **Initialize & Apply**:
   ```bash
   terraform init
   terraform plan -out=tfplan
   terraform apply tfplan
   ```
4. **Capture Outputs**:
   Terraform will output the static public IP (`public_ip`) and SSH command.

---

## 3. Server Configuration & Environment Setup

1. **Connect via SSH**:
   ```bash
   ssh -i <your-key.pem> ubuntu@<STATIC_PUBLIC_IP>
   ```

2. **Clone the Repository on the Cloud Host**:
   ```bash
   sudo git clone https://github.com/<your-username>/shosphere.git /opt/shopsphere
   sudo chown -R ubuntu:ubuntu /opt/shopsphere
   cd /opt/shopsphere
   ```

3. **Configure Production Secrets (`.env`)**:
   Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   nano .env
   ```
   Configure the production values:
   ```ini
   DOMAIN_NAME=shopsphere.yourdomain.com
   SSL_EMAIL=admin@yourdomain.com
   DB_PASSWORD=a_strong_random_mysql_root_password
   JWT_SECRET=8f2a1b9c8e4d3f2a1b9c8e4d3f2a1b9c8e4d3f2a1b9c8e4d3f2a1b9c8e4d3f2a
   INTERNAL_SERVICE_SECRET=shopsphere-internal-service-secret-key-384
   ```
   > [!IMPORTANT]
   > Ensure file permissions on `.env` are restricted to the owner:
   > `chmod 600 .env`

---

## 4. DNS & SSL / TLS Certificate Setup

1. **Configure DNS Records**:
   At your domain registrar or DNS provider (Route 53, Cloudflare, Namecheap):
   - Create an **A Record**: `shopsphere.yourdomain.com` pointing to `<STATIC_PUBLIC_IP>`.
   - Verify resolution: `dig +short shopsphere.yourdomain.com`.

2. **Initialize Automated Let's Encrypt TLS**:
   Execute the automated certificate bootstrap script:
   ```bash
   chmod +x scripts/*.sh
   ./scripts/init-letsencrypt.sh
   ```
   This script:
   - Provisions temporary bootstrap keys to start Nginx.
   - Solves the ACME webroot challenge via Certbot.
   - Replaces keys with genuine Let's Encrypt certificates.
   - Reloads Nginx with HTTP/2 and TLS 1.3 encryption.

---

## 5. Launching the Production Stack

Start all 14 containers using the production Compose specification:
```bash
docker compose -f docker-compose.prod.yml up -d --build
```

### Health Verification:
Run the automated production health check:
```bash
./scripts/health-check.sh
```
This verifies:
- All core containers are healthy (`service_healthy` status).
- The public edge Nginx reverse proxy responds on `/healthz`.
- The frontend SPA loads at `https://${DOMAIN_NAME}/`.
- The API Gateway is routing `/api/v1/products`.
- Internal microservice and database ports are strictly hidden from host network exposure.

---

## 6. Continuous Deployment (CI/CD) Setup

The project includes an enterprise-grade GitHub Actions pipeline at `.github/workflows/ci-cd.yml`.

### Configuring GitHub Repository Secrets:
Go to your GitHub repository -> **Settings** -> **Secrets and variables** -> **Actions** -> **New repository secret**:

| Secret Name | Value Description |
| :--- | :--- |
| `PROD_HOST` | Static Public IP address of the cloud host |
| `PROD_USERNAME` | `ubuntu` |
| `PROD_SSH_KEY` | Private SSH key (PEM format) matching the server's `authorized_keys` |
| `PROD_PORT` | `22` |

Once configured, any push or pull-request merge to `main` automatically runs all 141 backend tests, compiles the frontend bundle, verifies Compose specifications, and deploys zero-downtime updates via `scripts/deploy.sh`!

---

## 7. Database Backups, Disaster Recovery & Retention

### Running an On-Demand Backup:
```bash
./scripts/backup-databases.sh
```
- Dumps all 6 microservice databases into `/var/backups/shopsphere/<TIMESTAMP>/`.
- Creates compressed `.sql.gz` archives with SHA-256 integrity checksums.
- Automatically prunes snapshots older than 14 days.

### Setting Up Nightly Automated Backups:
Add a root cron job:
```bash
sudo crontab -e
```
Add the following line to run backups nightly at 02:00 AM:
```cron
0 2 * * * /opt/shopsphere/scripts/backup-databases.sh >> /var/log/shopsphere-backup.log 2>&1
```

### Restoring from a Backup:
```bash
./scripts/restore-databases.sh /var/backups/shopsphere/<TIMESTAMP>
```

---

## 8. Secure Observability Access (SSH Tunneling)

In production, Prometheus, Grafana, and Zipkin are deliberately shielded from the public internet. To access monitoring dashboards securely:

1. **Open an SSH Tunnel from your local machine**:
   ```bash
   ssh -i <your-key.pem> -L 3000:localhost:3000 -L 9090:localhost:9090 -L 9411:localhost:9411 ubuntu@<STATIC_PUBLIC_IP>
   ```
2. **Access Dashboards in your local browser**:
   - **Grafana**: [http://localhost:3000](http://localhost:3000) (User: `admin` / Password: `${GRAFANA_ADMIN_PASSWORD}`)
   - **Prometheus**: [http://localhost:9090](http://localhost:9090)
   - **Zipkin Distributed Tracing**: [http://localhost:9411](http://localhost:9411)

---

## 9. Operational Maintenance & Log Inspection

### Viewing Service Logs:
```bash
# View live API Gateway logs
docker compose -f docker-compose.prod.yml logs -f api-gateway

# View live Order Service logs
docker compose -f docker-compose.prod.yml logs -f order-service

# View Nginx access & error logs
docker compose -f docker-compose.prod.yml logs -f nginx-proxy
```

### Safe Graceful Shutdown (Preserving Data):
```bash
docker compose -f docker-compose.prod.yml down
```
> [!CAUTION]
> Never run `docker compose down -v` on production, as it deletes the persistent MySQL volume!
