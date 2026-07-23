output "vpc_id" {
  value = aws_vpc.photo_storage_vpc.id
}

output "public_subnet_ids" {
  value = [aws_subnet.public_subnet_1.id, aws_subnet.public_subnet_2.id]
}

output "private_subnet_ids" {
  value = [aws_subnet.private_subnet_1.id, aws_subnet.private_subnet_2.id]
}

output "db_subnet_ids" {
  value = [aws_subnet.db_subnet_1.id, aws_subnet.db_subnet_2.id]
}

output "db_subnet_group_name" {
  value = aws_db_subnet_group.photo_storage_db_subnet_group.name
}

output "eks_cluster_name" {
  value = aws_eks_cluster.photo_storage_eks.name
}

output "eks_cluster_endpoint" {
  value = aws_eks_cluster.photo_storage_eks.endpoint
}

output "rds_endpoint" {
  value = aws_db_instance.photo_storage_db.endpoint
}

output "photo_storage_s3_bucket_name" {
  value = aws_s3_bucket.photo_storage_s3_bucket.bucket
}

output "frontend_ecr_repository_url" {
  value = aws_ecr_repository.frontend.repository_url
}

output "backend_ecr_repository_url" {
  value = aws_ecr_repository.backend.repository_url
}

output "photo_storage_s3_bucket_arn" {
  value = aws_s3_bucket.photo_storage_s3_bucket.arn
}

output "backend_irsa_role_arn" {
  value = aws_iam_role.backend_irsa_role.arn
}

output "aws_load_balancer_controller_role_arn" {
  value = aws_iam_role.aws_load_balancer_controller_role.arn
}

output "eks_oidc_issuer_url" {
  value = aws_eks_cluster.photo_storage_eks.identity[0].oidc[0].issuer
}