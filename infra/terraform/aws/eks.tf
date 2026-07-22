resource "aws_eks_cluster" "photo_storage_eks" {
  name     = "photo-storage-eks"
  role_arn = aws_iam_role.eks_cluster_role.arn

  vpc_config {
    subnet_ids = [
      aws_subnet.public_subnet_1.id,
      aws_subnet.public_subnet_2.id,
      aws_subnet.private_subnet_1.id,
      aws_subnet.private_subnet_2.id
    ]

    endpoint_private_access = true
    endpoint_public_access  = true
    security_group_ids      = [aws_security_group.photo_storage_app_sg.id]
  }

  depends_on = [aws_iam_role_policy_attachment.eks_cluster_role_attachment]

  tags = {
    Name = "photo-storage-eks"
  }
}

resource "aws_eks_node_group" "photo_storage_eks_node_group" {
  cluster_name    = aws_eks_cluster.photo_storage_eks.name
  node_group_name = "photo-storage-eks-node-group"
  node_role_arn   = aws_iam_role.eks_node_group_role.arn
  subnet_ids = [
    aws_subnet.private_subnet_1.id,
    aws_subnet.private_subnet_2.id
  ]

  scaling_config {
    desired_size = 1
    max_size     = 2
    min_size     = 1
  }

  instance_types = ["t3.small"]

  depends_on = [
    aws_iam_role_policy_attachment.eks_worker_node_policy,
    aws_iam_role_policy_attachment.eks_cni_policy,
    aws_iam_role_policy_attachment.ecr_read_only_policy
  ]

  tags = {
    Name = "photo-storage-eks-node-group"
  }
}

data "tls_certificate" "eks_oidc" {
  url = aws_eks_cluster.photo_storage_eks.identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "eks_oidc_provider" {
  url = aws_eks_cluster.photo_storage_eks.identity[0].oidc[0].issuer

  client_id_list = [
    "sts.amazonaws.com"
  ]

  thumbprint_list = [
    data.tls_certificate.eks_oidc.certificates[0].sha1_fingerprint
  ]

  tags = {
    Name = "photo-storage-eks-oidc-provider"
  }
}
