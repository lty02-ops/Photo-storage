resource "google_project_service" "artifact_registry" {
  project = var.project_id
  service = "artifactregistry.googleapis.com"

  disable_on_destroy = false
}

resource "google_artifact_registry_repository" "my_repository" {
  project       = var.project_id
  location      = var.region
  repository_id = "photo-storage"
  description   = "Artifact Registry for photo storage"
  format        = "DOCKER"

  depends_on = [
    google_project_service.artifact_registry
  ]
}