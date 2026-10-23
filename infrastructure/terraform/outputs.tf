output "public_ip" {
  description = "The static public IP address of the ShopSphere production server"
  value       = aws_eip.shopsphere_eip.public_ip
}

output "instance_id" {
  description = "The EC2 instance ID"
  value       = aws_instance.shopsphere_host.id
}

output "ssh_command" {
  description = "Command to establish administrative SSH connection"
  value       = "ssh -i <path-to-key.pem> ubuntu@${aws_eip.shopsphere_eip.public_ip}"
}

output "dns_setup_instructions" {
  description = "DNS A-Record setup instruction for custom domain"
  value       = "Create an A-Record for ${var.domain_name} pointing to ${aws_eip.shopsphere_eip.public_ip}"
}
