resource "google_project_service" "container_api" {
  project = var.project_id
  service = "container.googleapis.com"

  disable_on_destroy = false
}

resource "google_container_cluster" "photo_storage" {
  name     = "photo-storage-cluster"
  location = var.zone

  remove_default_node_pool = true
  initial_node_count       = 1

  network    = google_compute_network.photo_storage.id
  subnetwork = google_compute_subnetwork.gke.id

  ip_allocation_policy {
    cluster_secondary_range_name  = "photo-storage-pods"
    services_secondary_range_name = "photo-storage-services"
  }

  workload_identity_config {
    workload_pool = "${var.project_id}.svc.id.goog"
  }

  deletion_protection = false

  depends_on = [
    google_project_service.container_api
  ]
}

resource "google_container_node_pool" "primary" {
  name       = "primary-node-pool"
  cluster    = google_container_cluster.photo_storage.name
  location   = var.zone
  node_count = 1


  node_config {
    machine_type    = "e2-medium"
    service_account = google_service_account.gke_nodes.email

    workload_metadata_config {
      mode = "GKE_METADATA"
    }

    oauth_scopes = [
      "https://www.googleapis.com/auth/cloud-platform"
    ]

    labels = {
      "app" = "photo-storage"
    }
  }
}

resource "google_service_account" "gke_nodes" {
  project      = var.project_id
  account_id   = "photo-storage-gke-nodes"
  display_name = "photo-storage GKE Nodes"
}

resource "google_project_iam_member" "gke_nodes_artifact_reader" {
  project = var.project_id
  role    = "roles/artifactregistry.reader"
  member  = "serviceAccount:${google_service_account.gke_nodes.email}"
}

resource "google_project_iam_member" "gke_nodes_default_role" {
  project = var.project_id
  role    = "roles/container.defaultNodeServiceAccount"
  member  = "serviceAccount:${google_service_account.gke_nodes.email}"
}
