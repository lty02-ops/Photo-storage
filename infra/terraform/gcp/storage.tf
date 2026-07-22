resource "google_storage_bucket" "my_bucket" {
  name     = "${var.project_id}-photo-storage-bucket"
  location = var.region

  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"

  force_destroy = true

  versioning {
    enabled = true
  }

  lifecycle_rule {
    action {
      type = "Delete"
    }
    condition {
      age        = 30
      with_state = "ARCHIVED"
    }
  }
}
