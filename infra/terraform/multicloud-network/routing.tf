resource "aws_vpn_gateway_route_propagation" "private" {
  vpn_gateway_id = aws_vpn_gateway.photo_storage.id
  route_table_id = var.aws_private_route_table_id
}

resource "aws_vpn_gateway_route_propagation" "database" {
  vpn_gateway_id = aws_vpn_gateway.photo_storage.id
  route_table_id = var.aws_db_route_table_id
}