variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "region" {
  description = "The GCP region to deploy resources in"
  type        = string
  default     = "asia-northeast3"
}

variable "aws_eks_oidc_issuer_url" {
  description = "AWS EKS OIDC issuer URL"
  type        = string
}
