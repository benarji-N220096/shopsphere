variable "aws_region" {
  description = "The AWS deployment region for ShopSphere production infrastructure"
  type        = string
  default     = "ap-south-1" # Mumbai (low latency, cost-effective)
}

variable "environment" {
  description = "Deployment environment name"
  type        = string
  default     = "production"
}

variable "instance_type" {
  description = "EC2 instance type for ShopSphere microservices workload"
  type        = string
  default     = "t3.xlarge" # 4 vCPUs, 16 GB RAM (optimal for 11 containers + MySQL)
}

variable "disk_size_gb" {
  description = "Root gp3 volume size in GB"
  type        = number
  default     = 60
}

variable "key_name" {
  description = "The name of the AWS EC2 Key Pair used for administrative SSH access"
  type        = string
}

variable "admin_ssh_cidr" {
  description = "CIDR block permitted to establish SSH connections (restrict to administrator IP)"
  type        = string
  default     = "0.0.0.0/0"
}

variable "domain_name" {
  description = "The domain name pointing to the ShopSphere application instance"
  type        = string
  default     = "shopsphere.example.com"
}
