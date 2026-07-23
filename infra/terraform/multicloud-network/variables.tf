variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "gcp_project_id" {
  type = string
}

variable "gcp_region" {
  type    = string
  default = "asia-northeast3"
}

variable "aws_vpc_id" {
  type = string
}

variable "aws_private_route_table_id" {
  type = string
}

variable "aws_db_route_table_id" {
  type = string
}

variable "gcp_network_id" {
  type = string
}

variable "aws_vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "gcp_node_cidr" {
  type    = string
  default = "10.10.0.0/20"
}

variable "gcp_pod_cidr" {
  type    = string
  default = "10.20.0.0/16"
}

variable "cloud_sql_cidr" {
  type    = string
  default = "10.40.0.0/16"
}

variable "gcp_router_asn" {
  type    = number
  default = 64514
}

variable "aws_vpn_asn" {
  type    = number
  default = 64512
}