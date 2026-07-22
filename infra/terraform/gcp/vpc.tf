resource "google_compute_network" "photo_storage" {
  name                    = "photo-storage-vpc"
  auto_create_subnetworks = false
  project                 = var.project_id

  depends_on = [
    google_project_service.compute_api
  ]
}

resource "google_compute_subnetwork" "gke" {
  name          = "photo-storage-subnet"
  ip_cidr_range = "10.10.0.0/20"
  region        = var.region
  network       = google_compute_network.photo_storage.id
  project       = var.project_id

  private_ip_google_access = true

  secondary_ip_range {
    range_name    = "photo-storage-pods"
    ip_cidr_range = "10.20.0.0/16"
  }

  secondary_ip_range {
    range_name    = "photo-storage-services"
    ip_cidr_range = "10.30.0.0/20"
  }
}

resource "google_project_service" "compute_api" {
  project = var.project_id
  service = "compute.googleapis.com"

  disable_on_destroy = false
}