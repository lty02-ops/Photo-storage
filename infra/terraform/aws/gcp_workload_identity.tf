data "aws_iam_policy_document" "gcp_backend_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = ["accounts.google.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "accounts.google.com:aud"
      values   = [var.gcp_backend_service_account_subject]
    }

    condition {
      test     = "StringEquals"
      variable = "accounts.google.com:oaud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "accounts.google.com:sub"
      values   = [var.gcp_backend_service_account_subject]
    }
  }
}

resource "aws_iam_role" "gcp_backend_s3_role" {
  count = var.gcp_backend_service_account_subject != "" ? 1 : 0

  name               = "photo-storage-gcp-backend-s3-role"
  assume_role_policy = data.aws_iam_policy_document.gcp_backend_assume_role.json
}

resource "aws_iam_role_policy_attachment" "gcp_backend_s3_access" {
  count = var.gcp_backend_service_account_subject != "" ? 1 : 0

  role       = aws_iam_role.gcp_backend_s3_role[count.index].name
  policy_arn = aws_iam_policy.backend_s3_policy.arn
}
