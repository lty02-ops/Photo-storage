output "gcp_vpn_gateway_ip_0" {
  value = google_compute_ha_vpn_gateway.photo_storage.vpn_interfaces[0].ip_address
}

output "gcp_vpn_gateway_ip_1" {
  value = google_compute_ha_vpn_gateway.photo_storage.vpn_interfaces[1].ip_address
}

output "aws_vpn_connection_ids" {
  value = [
    aws_vpn_connection.gcp_interface_0.id,
    aws_vpn_connection.gcp_interface_1.id
  ]
}