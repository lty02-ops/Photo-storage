resource "aws_db_instance" "photo_storage_db" {
  identifier = "photo-storage-db"

  engine         = "postgres"
  instance_class = "db.t3.micro"
  db_name        = "photo_storage"
  username       = var.db_username
  password       = var.db_password

  allocated_storage      = 20
  storage_type           = "gp3"
  publicly_accessible    = false
  db_subnet_group_name   = aws_db_subnet_group.photo_storage_db_subnet_group.name
  vpc_security_group_ids = [aws_security_group.photo_storage_db_sg.id]
  multi_az               = false

  skip_final_snapshot     = true
  backup_retention_period = 1
  apply_immediately       = true
  tags = {
    Name = "photo-storage-db"
  }

}
