resource "aws_s3_bucket" "photo_storage_s3_bucket" {
  bucket = "photo-storage-bucket-${var.environment}-${var.bucket_suffix}"

  tags = {
    Name        = "photo-storage-bucket"
    Environment = var.environment
  }
}

resource "aws_s3_bucket_public_access_block" "photo_storage_s3_bucket_public_access_block" {
  bucket = aws_s3_bucket.photo_storage_s3_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_versioning" "photo_storage_s3_bucket_versioning" {
  bucket = aws_s3_bucket.photo_storage_s3_bucket.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "photo_storage_s3_bucket_encryption" {
  bucket = aws_s3_bucket.photo_storage_s3_bucket.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "photo_storage_s3_bucket_lifecycle" {
  bucket = aws_s3_bucket.photo_storage_s3_bucket.id

  rule {
    id     = "ExpireOldVersions"
    status = "Enabled"

    filter {}

    noncurrent_version_expiration {
      noncurrent_days = 30
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}
