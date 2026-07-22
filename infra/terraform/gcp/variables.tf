variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "region" {
  description = "The GCP region to deploy resources in"
  type        = string
  default     = "asia-northeast3"
}

variable "zone" {
  type    = string
  default = "asia-northeast3-a"
}

variable "db_password" {
  description = "Cloud SQL PostgreSQL password"
  type        = string
  sensitive   = true
}