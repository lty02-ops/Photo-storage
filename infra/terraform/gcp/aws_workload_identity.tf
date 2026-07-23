data "google_project" "current" {
  project_id = var.project_id
}

resource "google_project_service" "iam_credentials_api" {
  project            = var.project_id
  service            = "iamcredentials.googleapis.com"
  disable_on_destroy = false
}

resource "google_iam_workload_identity_pool" "aws_eks" {
  workload_identity_pool_id = "photo-storage-aws-eks"
  display_name              = "Photo Storage AWS EKS"
}

resource "google_iam_workload_identity_pool_provider" "aws_eks" {
  workload_identity_pool_id          = google_iam_workload_identity_pool.aws_eks.workload_identity_pool_id
  workload_identity_pool_provider_id = "photo-storage-eks"
  display_name                       = "Photo Storage EKS"

  oidc {
    issuer_uri = var.aws_eks_oidc_issuer_url
  }

  attribute_mapping = {
    "google.subject" = "assertion.sub"
  }

  attribute_condition = <<-EOT
    assertion.sub == "system:serviceaccount:photo-storage:photo-storage-backend"
  EOT
}

resource "google_service_account_iam_member" "aws_backend_workload_identity" {
  service_account_id = google_service_account.backend.name
  role               = "roles/iam.workloadIdentityUser"

  member = "principal://iam.googleapis.com/projects/${data.google_project.current.number}/locations/global/workloadIdentityPools/${google_iam_workload_identity_pool.aws_eks.workload_identity_pool_id}/subject/system:serviceaccount:photo-storage:photo-storage-backend"

  depends_on = [google_project_service.iam_credentials_api]
}
