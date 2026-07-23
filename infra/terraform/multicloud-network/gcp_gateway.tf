resource "google_compute_ha_vpn_gateway" "photo_storage" {
  name    = "photo-storage-ha-vpn"
  project = var.gcp_project_id
  region  = var.gcp_region
  network = var.gcp_network_id
}

resource "google_compute_router" "photo_storage" {
  name    = "photo-storage-cloud-router"
  project = var.gcp_project_id
  region  = var.gcp_region
  network = var.gcp_network_id

  bgp {
    asn            = var.gcp_router_asn
    advertise_mode = "CUSTOM"

    advertised_groups = ["ALL_SUBNETS"]

    advertised_ip_ranges {
      range       = var.cloud_sql_cidr
      description = "Cloud SQL private service range"
    }
  }
}
