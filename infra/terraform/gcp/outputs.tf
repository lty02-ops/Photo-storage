output "photo_storage_bucket_name" {
  value = google_storage_bucket.my_bucket.name
}

output "project_number" {
  value = data.google_project.current.number
}

output "aws_workload_identity_provider" {
  value = google_iam_workload_identity_pool_provider.aws_eks.name
}

output "backend_service_account_email" {
  value = google_service_account.backend.email
}

output "backend_service_account_subject" {
  description = "Unique subject ID of the GCP backend service account"
  value       = google_service_account.backend.unique_id
}

output "network_id" {
  value = google_compute_network.photo_storage.id
}
