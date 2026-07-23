resource "aws_db_parameter_group" "photo_storage_postgres" {
  name   = "photo-storage-postgres15-logical-replication"
  family = "postgres15"

  parameter {
    name         = "shared_preload_libraries"
    value        = "pglogical"
    apply_method = "pending-reboot"
  }

  parameter {
    name         = "rds.logical_replication"
    value        = "1"
    apply_method = "pending-reboot"
  }

  parameter {
    name         = "wal_sender_timeout"
    value        = "0"
    apply_method = "pending-reboot"
  }

  parameter {
    name         = "max_replication_slots"
    value        = "10"
    apply_method = "pending-reboot"
  }

  parameter {
    name         = "max_wal_senders"
    value        = "10"
    apply_method = "pending-reboot"
  }

  parameter {
    name         = "max_worker_processes"
    value        = "10"
    apply_method = "pending-reboot"
  }

  tags = {
    Name = "photo-storage-postgres15-logical-replication"
  }
}

resource "aws_db_instance" "photo_storage_db" {
  identifier = "photo-storage-db"

  engine         = "postgres"
  engine_version = "15"

  parameter_group_name = aws_db_parameter_group.photo_storage_postgres.name

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
