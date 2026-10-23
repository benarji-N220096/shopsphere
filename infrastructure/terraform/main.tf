# ==============================================================================
# ShopSphere Production Cloud Infrastructure (Terraform)
# Provisions secure VPC, Subnet, Security Groups, EC2 Host, and Elastic IP
# ==============================================================================

terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.50"
    }
  }
}

provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project     = "ShopSphere"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# 1. VPC and Networking
resource "aws_vpc" "shopsphere_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "shopsphere-${var.environment}-vpc"
  }
}

resource "aws_internet_gateway" "shopsphere_igw" {
  vpc_id = aws_vpc.shopsphere_vpc.id

  tags = {
    Name = "shopsphere-${var.environment}-igw"
  }
}

resource "aws_subnet" "shopsphere_public_subnet" {
  vpc_id                  = aws_vpc.shopsphere_vpc.id
  cidr_block              = "10.0.1.0/24"
  map_public_ip_on_launch = true
  availability_zone       = "${var.aws_region}a"

  tags = {
    Name = "shopsphere-${var.environment}-public-subnet"
  }
}

resource "aws_route_table" "shopsphere_public_rt" {
  vpc_id = aws_vpc.shopsphere_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.shopsphere_igw.id
  }

  tags = {
    Name = "shopsphere-${var.environment}-public-rt"
  }
}

resource "aws_route_table_association" "shopsphere_public_rta" {
  subnet_id      = aws_subnet.shopsphere_public_subnet.id
  route_table_id = aws_route_table.shopsphere_public_rt.id
}

# 2. Security Group (Strict Least-Privilege Ingress)
resource "aws_security_group" "shopsphere_sg" {
  name        = "shopsphere-${var.environment}-sg"
  description = "Allows HTTP, HTTPS, and restricted SSH ingress to ShopSphere"
  vpc_id      = aws_vpc.shopsphere_vpc.id

  # HTTP (Port 80) - Required for Let's Encrypt validation and HTTP->HTTPS redirect
  ingress {
    description = "Public HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTPS (Port 443) - Secure application entry point
  ingress {
    description = "Public HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # SSH (Port 22) - Administrator access (restricted CIDR)
  ingress {
    description = "Administrative SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.admin_ssh_cidr]
  }

  # Egress - Unrestricted outbound internet
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "shopsphere-${var.environment}-sg"
  }
}

# 3. Ubuntu 24.04 LTS AMI Data Source
data "aws_ami" "ubuntu_latest" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# 4. Production Host EC2 Instance
resource "aws_instance" "shopsphere_host" {
  ami                    = data.aws_ami.ubuntu_latest.id
  instance_type          = var.instance_type
  key_name               = var.key_name
  subnet_id              = aws_subnet.shopsphere_public_subnet.id
  vpc_security_group_ids = [aws_security_group.shopsphere_sg.id]

  user_data = file("${path.module}/cloud-init.yaml")

  root_block_device {
    volume_size           = var.disk_size_gb
    volume_type           = "gp3"
    encrypted             = true
    delete_on_termination = false # Prevents accidental data loss

    tags = {
      Name = "shopsphere-${var.environment}-root-volume"
    }
  }

  tags = {
    Name = "shopsphere-${var.environment}-server"
  }
}

# 5. Static Elastic IP
resource "aws_eip" "shopsphere_eip" {
  instance = aws_instance.shopsphere_host.id
  domain   = "vpc"

  tags = {
    Name = "shopsphere-${var.environment}-eip"
  }
}
