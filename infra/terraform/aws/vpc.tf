resource "aws_vpc" "photo_storage_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true
}

resource "aws_subnet" "public_subnet_1" {
  vpc_id                  = aws_vpc.photo_storage_vpc.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "ap-northeast-2a"
  map_public_ip_on_launch = true
  tags = {
    Name                     = "public-subnet-1"
    "kubernetes.io/role/elb" = "1"
  }
}

resource "aws_subnet" "public_subnet_2" {
  vpc_id                  = aws_vpc.photo_storage_vpc.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "ap-northeast-2b"
  map_public_ip_on_launch = true
  tags = {
    Name                     = "public-subnet-2"
    "kubernetes.io/role/elb" = "1"
  }
}

resource "aws_subnet" "private_subnet_1" {
  vpc_id            = aws_vpc.photo_storage_vpc.id
  cidr_block        = "10.0.3.0/24"
  availability_zone = "ap-northeast-2a"
  tags = {
    Name                              = "private-subnet-1"
    "kubernetes.io/role/internal-elb" = "1"
  }
}

resource "aws_subnet" "private_subnet_2" {
  vpc_id            = aws_vpc.photo_storage_vpc.id
  cidr_block        = "10.0.4.0/24"
  availability_zone = "ap-northeast-2b"
  tags = {
    Name                              = "private-subnet-2"
    "kubernetes.io/role/internal-elb" = "1"
  }
}

resource "aws_subnet" "db_subnet_1" {
  vpc_id            = aws_vpc.photo_storage_vpc.id
  cidr_block        = "10.0.5.0/24"
  availability_zone = "ap-northeast-2a"
}

resource "aws_subnet" "db_subnet_2" {
  vpc_id            = aws_vpc.photo_storage_vpc.id
  cidr_block        = "10.0.6.0/24"
  availability_zone = "ap-northeast-2b"
}

resource "aws_internet_gateway" "photo_storage_igw" {
  vpc_id = aws_vpc.photo_storage_vpc.id
}

resource "aws_eip" "photo_storage_eip" {
  domain = "vpc"
}

resource "aws_nat_gateway" "photo_storage_nat_gw" {
  allocation_id = aws_eip.photo_storage_eip.id
  subnet_id     = aws_subnet.public_subnet_1.id
}

resource "aws_route_table" "public_route_table" {
  vpc_id = aws_vpc.photo_storage_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.photo_storage_igw.id
  }
}

resource "aws_route_table" "private_route_table" {
  vpc_id = aws_vpc.photo_storage_vpc.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.photo_storage_nat_gw.id
  }
}

resource "aws_route_table" "db_route_table" {
  vpc_id = aws_vpc.photo_storage_vpc.id
}

resource "aws_route_table_association" "public_subnet_1_association" {
  subnet_id      = aws_subnet.public_subnet_1.id
  route_table_id = aws_route_table.public_route_table.id
}

resource "aws_route_table_association" "public_subnet_2_association" {
  subnet_id      = aws_subnet.public_subnet_2.id
  route_table_id = aws_route_table.public_route_table.id
}

resource "aws_route_table_association" "private_subnet_1_association" {
  subnet_id      = aws_subnet.private_subnet_1.id
  route_table_id = aws_route_table.private_route_table.id
}

resource "aws_route_table_association" "private_subnet_2_association" {
  subnet_id      = aws_subnet.private_subnet_2.id
  route_table_id = aws_route_table.private_route_table.id
}

resource "aws_route_table_association" "db_subnet_1_association" {
  subnet_id      = aws_subnet.db_subnet_1.id
  route_table_id = aws_route_table.db_route_table.id
}

resource "aws_route_table_association" "db_subnet_2_association" {
  subnet_id      = aws_subnet.db_subnet_2.id
  route_table_id = aws_route_table.db_route_table.id
}

resource "aws_db_subnet_group" "photo_storage_db_subnet_group" {
  name       = "photo-storage-db-subnet-group"
  subnet_ids = [aws_subnet.db_subnet_1.id, aws_subnet.db_subnet_2.id]

  tags = {
    Name = "photo-storage-db-subnet-group"
  }
}

