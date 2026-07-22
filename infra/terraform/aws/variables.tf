variable "region" {
  description = "The AWS region to deploy resources in"
  type        = string
  default     = "ap-northeast-2"
}

variable "db_username" {
  description = "RDS PostgreSQL username"
  type        = string
  default     = "photo_user"
}

variable "db_password" {
  description = "RDS PostgreSQL password"
  type        = string
  sensitive   = true
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "dev"
}

variable "bucket_suffix" {
  description = "Unique suffix for globally unique S3 bucket name"
  type        = string
  default     = "lty02"
}