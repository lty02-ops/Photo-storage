output "photo_storage_bucket_name" {
  value = google_storage_bucket.my_bucket.name
}

output "cloud_sql_private_ip" {
  value = google_sql_database_instance.photo_storage.private_ip_address
}

output "cloud_sql_connection_name" {
  value = google_sql_database_instance.photo_storage.connection_name
}