resource "google_service_account" "backend" {
  project      = var.project_id
  account_id   = "photo-storage-backend"
  display_name = "Photo Storage Backend"
}

resource "google_storage_bucket_iam_member" "backend_storage_access" {
  bucket = google_storage_bucket.my_bucket.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.backend.email}"
}

resource "google_service_account_iam_member" "backend_workload_identity" {
  service_account_id = google_service_account.backend.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.project_id}.svc.id.goog[photo-storage/photo-storage-backend]"

  depends_on = [
    google_container_cluster.photo_storage
  ]
}
