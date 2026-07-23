resource "google_compute_external_vpn_gateway" "aws" {
  name            = "photo-storage-aws-vpn-gateway"
  project         = var.gcp_project_id
  redundancy_type = "FOUR_IPS_REDUNDANCY"

  interface {
    id         = 0
    ip_address = aws_vpn_connection.gcp_interface_0.tunnel1_address
  }

  interface {
    id         = 1
    ip_address = aws_vpn_connection.gcp_interface_0.tunnel2_address
  }

  interface {
    id         = 2
    ip_address = aws_vpn_connection.gcp_interface_1.tunnel1_address
  }

  interface {
    id         = 3
    ip_address = aws_vpn_connection.gcp_interface_1.tunnel2_address
  }
}

locals {
  vpn_tunnels = {
    "0" = {
      gcp_interface      = 0
      aws_interface      = 0
      shared_secret      = aws_vpn_connection.gcp_interface_0.tunnel1_preshared_key
      gcp_inside_address = aws_vpn_connection.gcp_interface_0.tunnel1_cgw_inside_address
      aws_inside_address = aws_vpn_connection.gcp_interface_0.tunnel1_vgw_inside_address
    }
    "1" = {
      gcp_interface      = 0
      aws_interface      = 1
      shared_secret      = aws_vpn_connection.gcp_interface_0.tunnel2_preshared_key
      gcp_inside_address = aws_vpn_connection.gcp_interface_0.tunnel2_cgw_inside_address
      aws_inside_address = aws_vpn_connection.gcp_interface_0.tunnel2_vgw_inside_address
    }
    "2" = {
      gcp_interface      = 1
      aws_interface      = 2
      shared_secret      = aws_vpn_connection.gcp_interface_1.tunnel1_preshared_key
      gcp_inside_address = aws_vpn_connection.gcp_interface_1.tunnel1_cgw_inside_address
      aws_inside_address = aws_vpn_connection.gcp_interface_1.tunnel1_vgw_inside_address
    }
    "3" = {
      gcp_interface      = 1
      aws_interface      = 3
      shared_secret      = aws_vpn_connection.gcp_interface_1.tunnel2_preshared_key
      gcp_inside_address = aws_vpn_connection.gcp_interface_1.tunnel2_cgw_inside_address
      aws_inside_address = aws_vpn_connection.gcp_interface_1.tunnel2_vgw_inside_address
    }
  }
}

resource "google_compute_vpn_tunnel" "aws" {
  for_each = local.vpn_tunnels

  name                            = "photo-storage-aws-tunnel-${each.key}"
  project                         = var.gcp_project_id
  region                          = var.gcp_region
  vpn_gateway                     = google_compute_ha_vpn_gateway.photo_storage.id
  vpn_gateway_interface           = each.value.gcp_interface
  peer_external_gateway           = google_compute_external_vpn_gateway.aws.id
  peer_external_gateway_interface = each.value.aws_interface
  shared_secret                   = each.value.shared_secret
  router                          = google_compute_router.photo_storage.id
  ike_version                     = 2
}

resource "google_compute_router_interface" "aws" {
  for_each = local.vpn_tunnels

  name       = "photo-storage-aws-interface-${each.key}"
  project    = var.gcp_project_id
  region     = var.gcp_region
  router     = google_compute_router.photo_storage.name
  ip_range   = "${each.value.gcp_inside_address}/30"
  vpn_tunnel = google_compute_vpn_tunnel.aws[each.key].name
}

resource "google_compute_router_peer" "aws" {
  for_each = local.vpn_tunnels

  name                      = "photo-storage-aws-peer-${each.key}"
  project                   = var.gcp_project_id
  region                    = var.gcp_region
  router                    = google_compute_router.photo_storage.name
  interface                 = google_compute_router_interface.aws[each.key].name
  peer_ip_address           = each.value.aws_inside_address
  peer_asn                  = var.aws_vpn_asn
  advertised_route_priority = 100
}
