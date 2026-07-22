resource "google_project_service" "sql_admin_api" {
  project = var.project_id
  service = "sqladmin.googleapis.com"

  disable_on_destroy = false
}

resource "google_project_service" "service_networking_api" {
  project = var.project_id
  service = "servicenetworking.googleapis.com"

  disable_on_destroy = false
}

resource "google_compute_global_address" "cloud_sql_private_range" {
  name          = "photo-storage-cloud-sql-range"
  project       = var.project_id
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  address       = "10.40.0.0"
  prefix_length = 16
  network       = google_compute_network.photo_storage.id

  depends_on = [
    google_project_service.compute_api
  ]
}

resource "google_service_networking_connection" "cloud_sql_private_connection" {
  network                 = google_compute_network.photo_storage.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.cloud_sql_private_range.name]

  depends_on = [
    google_project_service.service_networking_api
  ]
}

resource "google_sql_database_instance" "photo_storage" {
  name             = "${var.project_id}-photo-storage-db"
  project          = var.project_id
  region           = var.region
  database_version = "POSTGRES_15"

  deletion_protection = false

  settings {

    tier              = "db-f1-micro"
    availability_type = "ZONAL"

    disk_type       = "PD_SSD"
    disk_size       = 10
    disk_autoresize = true


    ip_configuration {
      ipv4_enabled    = false
      private_network = google_compute_network.photo_storage.id
    }

    backup_configuration {
      enabled = false
    }
  }

  depends_on = [
    google_project_service.sql_admin_api,
    google_service_networking_connection.cloud_sql_private_connection
  ]
}

resource "google_sql_database" "photo_storage" {
  name     = "photo_storage"
  project  = var.project_id
  instance = google_sql_database_instance.photo_storage.name
}

resource "google_sql_user" "photo_user" {
  name            = "photo_user"
  project         = var.project_id
  instance        = google_sql_database_instance.photo_storage.name
  password        = var.db_password
  deletion_policy = "ABANDON"
}
