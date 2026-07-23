resource "aws_vpn_gateway" "photo_storage" {
  vpc_id          = var.aws_vpc_id
  amazon_side_asn = var.aws_vpn_asn

  tags = {
    Name = "photo-storage-vpn-gateway"
  }
}

resource "aws_customer_gateway" "gcp_interface_0" {
  bgp_asn    = var.gcp_router_asn
  ip_address = google_compute_ha_vpn_gateway.photo_storage.vpn_interfaces[0].ip_address
  type       = "ipsec.1"

  tags = {
    Name = "photo-storage-gcp-interface-0"
  }
}

resource "aws_customer_gateway" "gcp_interface_1" {
  bgp_asn    = var.gcp_router_asn
  ip_address = google_compute_ha_vpn_gateway.photo_storage.vpn_interfaces[1].ip_address
  type       = "ipsec.1"

  tags = {
    Name = "photo-storage-gcp-interface-1"
  }
}

resource "aws_vpn_connection" "gcp_interface_0" {
  customer_gateway_id = aws_customer_gateway.gcp_interface_0.id
  vpn_gateway_id      = aws_vpn_gateway.photo_storage.id
  type                = "ipsec.1"
  static_routes_only  = false

  tunnel1_ike_versions                 = ["ikev2"]
  tunnel2_ike_versions                 = ["ikev2"]
  tunnel1_phase1_encryption_algorithms = ["AES256"]
  tunnel2_phase1_encryption_algorithms = ["AES256"]
  tunnel1_phase2_encryption_algorithms = ["AES256"]
  tunnel2_phase2_encryption_algorithms = ["AES256"]
  tunnel1_phase1_integrity_algorithms  = ["SHA2-256"]
  tunnel2_phase1_integrity_algorithms  = ["SHA2-256"]
  tunnel1_phase2_integrity_algorithms  = ["SHA2-256"]
  tunnel2_phase2_integrity_algorithms  = ["SHA2-256"]
  tunnel1_phase1_dh_group_numbers      = [14]
  tunnel2_phase1_dh_group_numbers      = [14]
  tunnel1_phase2_dh_group_numbers      = [14]
  tunnel2_phase2_dh_group_numbers      = [14]

  tags = {
    Name = "photo-storage-gcp-vpn-0"
  }
}

resource "aws_vpn_connection" "gcp_interface_1" {
  customer_gateway_id = aws_customer_gateway.gcp_interface_1.id
  vpn_gateway_id      = aws_vpn_gateway.photo_storage.id
  type                = "ipsec.1"
  static_routes_only  = false

  tunnel1_ike_versions                 = ["ikev2"]
  tunnel2_ike_versions                 = ["ikev2"]
  tunnel1_phase1_encryption_algorithms = ["AES256"]
  tunnel2_phase1_encryption_algorithms = ["AES256"]
  tunnel1_phase2_encryption_algorithms = ["AES256"]
  tunnel2_phase2_encryption_algorithms = ["AES256"]
  tunnel1_phase1_integrity_algorithms  = ["SHA2-256"]
  tunnel2_phase1_integrity_algorithms  = ["SHA2-256"]
  tunnel1_phase2_integrity_algorithms  = ["SHA2-256"]
  tunnel2_phase2_integrity_algorithms  = ["SHA2-256"]
  tunnel1_phase1_dh_group_numbers      = [14]
  tunnel2_phase1_dh_group_numbers      = [14]
  tunnel1_phase2_dh_group_numbers      = [14]
  tunnel2_phase2_dh_group_numbers      = [14]

  tags = {
    Name = "photo-storage-gcp-vpn-1"
  }
}
