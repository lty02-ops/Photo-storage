resource "aws_security_group" "photo_storage_alb_sg" {
  name        = "photo-storage-alb-sg"
  description = "Security group for the Application Load Balancer"
  vpc_id      = aws_vpc.photo_storage_vpc.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]

  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "photo-storage-alb-sg"
  }
}

resource "aws_security_group" "photo_storage_app_sg" {
  name        = "photo-storage-app-sg"
  description = "Security group for the application instances"
  vpc_id      = aws_vpc.photo_storage_vpc.id

  ingress {
    from_port       = 80
    to_port         = 80
    protocol        = "tcp"
    security_groups = [aws_security_group.photo_storage_alb_sg.id]
  }

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.photo_storage_alb_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "photo-storage-app-sg"
  }
}

resource "aws_security_group" "photo_storage_db_sg" {
  name        = "photo-storage-db-sg"
  description = "Security group for the RDS database"
  vpc_id      = aws_vpc.photo_storage_vpc.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_eks_cluster.photo_storage_eks.vpc_config[0].cluster_security_group_id]
  }

  ingress {
    description = "PostgreSQL from GKE and Cloud SQL over the multi-cloud VPN"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [
      "10.10.0.0/20",
      "10.20.0.0/16",
      "10.40.0.0/16"
    ]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "photo-storage-db-sg"
  }
}
