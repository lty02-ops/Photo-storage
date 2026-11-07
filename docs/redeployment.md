# AWS·GCP 배포 순서

WSL의 Bash 기준입니다. 같은 터미널에서 순서대로 실행하고, 각 단계가 성공하면 다음 단계로 진행하세요. 이 안내는 명령을 실행하지 않으며, 실제 배포는 사용자가 `apply`와 Helm 명령을 실행할 때 시작됩니다.

전체 순서: **인증·결제 → GCP 서비스 계정 → AWS → GCP 전체 → VPN → 이미지 업로드 → Helm → 새 접속 주소·로그인 설정**.

Terraform은 인프라를, Helm은 앱을 배포합니다. Docker는 이미지 빌드·푸시 시에 필요합니다. `terraform apply`부터 EKS·GKE·RDS·VPN 등에 비용이 발생합니다. 이전 데이터는 삭제됐으므로 재배포해도 복원되지 않습니다.

## 1. 준비와 인증

필요한 도구: Terraform, AWS CLI, gcloud, kubectl, Helm, Docker, `gke-gcloud-auth-plugin`.

```bash
cd /mnt/d/Photo_Storage
export PROJECT_ROOT="$PWD"
export AWS_PAGER=""
export GCP_PROJECT_ID="photo-storage-503901"
export GCP_REGION="asia-northeast3"
export AWS_REGION="ap-northeast-2"

aws sts get-caller-identity
gcloud auth list
gcloud config set project "$GCP_PROJECT_ID"
gcloud billing projects describe "$GCP_PROJECT_ID"
```

계정이 다르거나 인증이 만료됐다면 다시 로그인합니다. gcloud CLI와 Terraform용 ADC 인증은 별도입니다. 브라우저에서 같은 계정을 선택하세요.

```bash
gcloud auth login --no-launch-browser
gcloud auth application-default login --no-launch-browser
gcloud auth application-default set-quota-project "$GCP_PROJECT_ID"
```

2026-11-05 종료 작업에서 이 GCP 프로젝트의 결제 연결을 해제했습니다. `billingEnabled: false`이면 [결제 콘솔](https://console.cloud.google.com/billing)에서 프로젝트를 활성 결제 계정에 연결합니다. CLI로 연결하려면 아래 명령에서 실제 결제 계정 ID를 사용합니다.

```bash
gcloud billing accounts list
gcloud billing projects link "$GCP_PROJECT_ID" --billing-account=실제_결제계정_ID
gcloud billing projects describe "$GCP_PROJECT_ID"
```

`billingEnabled: true`를 확인합니다. [공식 결제 연결 안내](https://docs.cloud.google.com/billing/docs/how-to/modify-project)

## 2. GCP 서비스 계정만 먼저 생성

AWS 구성은 GCP 서비스 계정 고유 ID를, GCP 전체 구성은 AWS EKS OIDC 주소를 요구합니다. 이 상호 의존성을 풀기 위해 서비스 계정만 먼저 생성합니다.

```bash
export TF_VAR_project_id="$GCP_PROJECT_ID"
terraform -chdir="$PROJECT_ROOT/infra/terraform/gcp" init
terraform -chdir="$PROJECT_ROOT/infra/terraform/gcp" apply \
  -target=google_service_account.backend \
  -var='aws_eks_oidc_issuer_url=bootstrap'

export TF_VAR_gcp_backend_service_account_subject="$(terraform -chdir="$PROJECT_ROOT/infra/terraform/gcp" output -raw backend_service_account_subject)"
```

`bootstrap`은 이 단계에만 쓰는 임시 입력값입니다. 이후에는 실제 EKS 주소를 사용하고 전체 구성을 적용합니다. 서비스 계정을 재생성하면 고유 ID가 달라질 수 있으므로 이전 ID를 복사하지 마세요.

## 3. AWS 인프라

입력하는 비밀번호는 새 RDS의 DB 비밀번호이며 AWS 계정 비밀번호가 아닙니다. 앱 설정에도 같은 값을 사용합니다.

```bash
export TF_VAR_bucket_suffix="$GCP_PROJECT_ID"
read -rsp "새 RDS DB 비밀번호: " TF_VAR_db_password
echo
export TF_VAR_db_password

terraform -chdir="$PROJECT_ROOT/infra/terraform/aws" init
terraform -chdir="$PROJECT_ROOT/infra/terraform/aws" plan -out=tfplan
terraform -chdir="$PROJECT_ROOT/infra/terraform/aws" apply tfplan

export TF_VAR_aws_eks_oidc_issuer_url="$(terraform -chdir="$PROJECT_ROOT/infra/terraform/aws" output -raw eks_oidc_issuer_url)"
```

`plan`의 대상을 확인한 뒤 저장한 계획을 적용하세요. S3 버킷 이름 중복 오류가 나면 `TF_VAR_bucket_suffix`를 고유한 값으로 바꾸고 계획부터 다시 확인합니다.

## 4. 최근 삭제한 인증 풀 복구와 GCP 전체 배포

GCP Workload Identity Pool·Provider는 삭제 후 30일 동안 이름을 재사용할 수 없습니다. 최근 삭제한 프로젝트를 재사용한다면 먼저 확인하세요. [공식 복구 안내](https://docs.cloud.google.com/iam/docs/manage-workload-identity-pools-providers)

```bash
gcloud iam workload-identity-pools list \
  --location=global --project="$GCP_PROJECT_ID" --show-deleted
```

`photo-storage-aws-eks`가 `DELETED`이면 GCP 콘솔의 **IAM 및 관리자 → Workload Identity Federation**에서 삭제된 풀 표시를 켜고 해당 풀을 복원합니다. 내부 `photo-storage-eks` Provider도 삭제 상태이면 복원합니다.

두 리소스가 활성 상태이며 Terraform state에서는 빠져 있는 경우에만 아래 import를 실행합니다. 처음 배포하거나 삭제된 풀이 없으면 복원·import를 건너뜁니다.

```bash
GCP_PROJECT_NUMBER="$(gcloud projects describe "$GCP_PROJECT_ID" --format='value(projectNumber)')"
terraform -chdir="$PROJECT_ROOT/infra/terraform/gcp" import \
  google_iam_workload_identity_pool.aws_eks \
  "projects/$GCP_PROJECT_NUMBER/locations/global/workloadIdentityPools/photo-storage-aws-eks"
terraform -chdir="$PROJECT_ROOT/infra/terraform/gcp" import \
  google_iam_workload_identity_pool_provider.aws_eks \
  "projects/$GCP_PROJECT_NUMBER/locations/global/workloadIdentityPools/photo-storage-aws-eks/providers/photo-storage-eks"
```

전체 구성을 적용합니다. 복원한 Provider의 이전 EKS 주소도 새 주소로 갱신됩니다.

```bash
terraform -chdir="$PROJECT_ROOT/infra/terraform/gcp" plan -out=tfplan
terraform -chdir="$PROJECT_ROOT/infra/terraform/gcp" apply tfplan
```

## 5. VPN 배포

GCP 앱은 VPN으로 AWS RDS에 접속하므로 이 단계를 완료한 뒤 앱을 배포합니다.

```bash
export TF_VAR_aws_vpc_id="$(terraform -chdir="$PROJECT_ROOT/infra/terraform/aws" output -raw vpc_id)"
export TF_VAR_aws_private_route_table_id="$(terraform -chdir="$PROJECT_ROOT/infra/terraform/aws" output -raw private_route_table_id)"
export TF_VAR_aws_db_route_table_id="$(terraform -chdir="$PROJECT_ROOT/infra/terraform/aws" output -raw db_route_table_id)"
export TF_VAR_gcp_network_id="$(terraform -chdir="$PROJECT_ROOT/infra/terraform/gcp" output -raw network_id)"
export TF_VAR_gcp_project_id="$GCP_PROJECT_ID"

terraform -chdir="$PROJECT_ROOT/infra/terraform/multicloud-network" init
terraform -chdir="$PROJECT_ROOT/infra/terraform/multicloud-network" plan -out=tfplan
terraform -chdir="$PROJECT_ROOT/infra/terraform/multicloud-network" apply tfplan

gcloud compute vpn-tunnels list --project="$GCP_PROJECT_ID" \
  --format='table(name,status,detailedStatus)'
gcloud compute routers get-status photo-storage-cloud-router \
  --region="$GCP_REGION" --project="$GCP_PROJECT_ID" \
  --format='yaml(result.bgpPeerStatus)'
```

터널 4개가 `ESTABLISHED`, BGP 4개가 `UP`인지 확인합니다.

## 6. 새 Terraform 값과 비밀번호 반영

```bash
terraform -chdir="$PROJECT_ROOT/infra/terraform/aws" output
terraform -chdir="$PROJECT_ROOT/infra/terraform/gcp" output

cd "$PROJECT_ROOT"
cp -n infra/helm/photo-storage/secret-values.example.yaml \
  infra/helm/photo-storage/secret-values.yaml
nano infra/helm/photo-storage/secret-values.yaml
```

`backend.env.datasourcePassword`에는 3단계의 RDS 비밀번호를 넣고 OAuth Client ID·Secret도 입력합니다. 카카오 `clientId`는 REST API 키입니다. `infra/.env`의 값은 Helm 설정에 자동 반영되지 않습니다.

기존 `values-aws.yaml`, `values-gcp.yaml`에는 과거 배포 값이 있으므로 다음 항목도 현재 출력값으로 바꿉니다. 비밀번호·OAuth Secret은 `secret-values.yaml`에만 보관합니다.

| Helm 설정 | 사용할 현재 값 |
|---|---|
| 양쪽 `backend.env.datasourceUrl` | AWS `rds_endpoint`로 만든 `jdbc:postgresql://출력값/photo_storage` |
| 양쪽 `backend.env.s3BucketName` | AWS `photo_storage_s3_bucket_name` |
| 양쪽 `backend.env.gcsBucketName` | GCP `photo_storage_bucket_name` |
| AWS `backend.serviceAccount.annotations.eks.amazonaws.com/role-arn` | AWS `backend_irsa_role_arn` |
| AWS `backend.gcpWorkloadIdentity.projectNumber` | GCP `project_number`, 문자열로 입력 |
| AWS `backend.gcpWorkloadIdentity.serviceAccountEmail` | GCP `backend_service_account_email` |
| GCP `backend.serviceAccount.annotations.iam.gke.io/gcp-service-account` | GCP `backend_service_account_email` |
| GCP `backend.awsWorkloadIdentity.roleArn` | AWS `gcp_backend_s3_role_arn` |
| 양쪽 `frontend.image.repository`, `backend.image.repository` | 실제 ECR·Artifact Registry 이미지 경로 |

`rds_endpoint`에는 이미 `:5432`가 포함되어 있으므로 포트를 중복해서 붙이지 마세요. GCP 서비스 계정을 새로 생성한 경우 AWS 신뢰 정책에 새 고유 ID가 반영돼야 합니다. 앞의 순서를 따르면 Terraform이 처리합니다.

## 7. 이미지 빌드·업로드

Docker를 켠 뒤 프로젝트 루트에서 [기존 배포 문서의 Docker image push](deployment.md#docker-image-push) 단계로 **프론트엔드와 백엔드를 ECR·Artifact Registry 양쪽에 업로드**합니다.

```bash
cd "$PROJECT_ROOT"
```

처음 배포하는 새 클러스터에서는 기존 문서의 `latest` 태그를 쓸 수 있습니다. 이후 업데이트에는 새 버전 태그를 사용하고 Helm의 `image.tag`도 일치시키세요. 같은 `latest`를 재푸시하면 `IfNotPresent` 설정 때문에 이전 이미지가 남을 수 있습니다.

## 8. EKS Controller 설치와 앱 배포

Terraform은 AWS Load Balancer Controller용 IAM 역할을 만들지만 Controller 앱 자체를 설치하지는 않습니다. [AWS 공식 설치 안내](https://docs.aws.amazon.com/eks/latest/userguide/lbc-helm.html)에 따라 Controller와 IAM 정책의 버전 호환성을 확인합니다.

```bash
cd "$PROJECT_ROOT"
aws eks update-kubeconfig --region "$AWS_REGION" \
  --name photo-storage-eks --alias photo-storage-aws
LBC_ROLE="$(terraform -chdir=infra/terraform/aws output -raw aws_load_balancer_controller_role_arn)"

helm repo add eks https://aws.github.io/eks-charts
helm repo update
helm upgrade --install aws-load-balancer-controller eks/aws-load-balancer-controller \
  --kube-context photo-storage-aws -n kube-system \
  --set clusterName=photo-storage-eks \
  --set region="$AWS_REGION" --set vpcId="$TF_VAR_aws_vpc_id" \
  --set serviceAccount.create=true \
  --set serviceAccount.name=aws-load-balancer-controller \
  --set-string "serviceAccount.annotations.eks\\.amazonaws\\.com/role-arn=$LBC_ROLE" \
  --wait --timeout 5m

helm upgrade --install photo-storage infra/helm/photo-storage \
  --kube-context photo-storage-aws -n photo-storage --create-namespace \
  -f infra/helm/photo-storage/values-aws.yaml \
  -f infra/helm/photo-storage/secret-values.yaml

kubectl --context photo-storage-aws annotate service photo-storage-backend \
  -n photo-storage alb.ingress.kubernetes.io/healthcheck-path=/actuator/health --overwrite
kubectl --context photo-storage-aws get pods,ingress -n photo-storage
```

서비스의 ALB 헬스 체크 경로는 백엔드에만 적용합니다. 프론트엔드는 기본 `/` 경로를 사용합니다.

## 9. GKE 앱 배포

```bash
cd "$PROJECT_ROOT"
gcloud container clusters get-credentials photo-storage-cluster \
  --region="$GCP_REGION" --project="$GCP_PROJECT_ID"
GKE_CONTEXT="gke_${GCP_PROJECT_ID}_${GCP_REGION}_photo-storage-cluster"

helm upgrade --install photo-storage infra/helm/photo-storage \
  --kube-context "$GKE_CONTEXT" -n photo-storage --create-namespace \
  -f infra/helm/photo-storage/values-gcp.yaml \
  -f infra/helm/photo-storage/secret-values.yaml \
  --set backend.healthCheck.port=8080

kubectl --context "$GKE_CONTEXT" get pods,ingress -n photo-storage
kubectl --context "$GKE_CONTEXT" get ingress photo-storage-ingress -n photo-storage \
  -o jsonpath='{.metadata.annotations.ingress\.kubernetes\.io/backends}'
echo
```

GKE가 NEG로 Pod에 직접 연결하는 경우 헬스 체크는 **8080**입니다. 서비스 유형이 NodePort여도 이 구성에서는 NodePort 번호로 바꾸지 않습니다. [GKE 공식 안내](https://docs.cloud.google.com/kubernetes-engine/docs/troubleshooting/ingress-health-checks)

Pod의 `Running` 외에 백엔드 로그의 `Started PhotoStorageApiApplication`, DB 연결 성공과 로드밸런서 `HEALTHY`도 확인합니다. 외부 주소와 헬스 상태 반영에는 몇 분 걸릴 수 있습니다.

## 10. 새 주소와 소셜 로그인

각 Ingress의 새 `ADDRESS`로 접속합니다. AWS는 ALB 호스트 이름, GCP는 IP로 나올 수 있습니다. 예전 IP `136.81.87.104`는 더 이상 사용하지 않습니다.

각각의 `values-aws.yaml`, `values-gcp.yaml`에서 `backend.env.frontendUrl`을 **해당 클러스터의 실제 접속 주소**로 바꾼 뒤 위 Helm 배포 명령을 다시 실행합니다. ConfigMap 변경을 앱이 읽도록 해당 클러스터에서 백엔드를 재시작합니다.

```bash
kubectl --context photo-storage-aws rollout restart deployment/photo-storage-backend -n photo-storage
kubectl --context "$GKE_CONTEXT" rollout restart deployment/photo-storage-backend -n photo-storage
```

소셜 로그인 개발자 콘솔에도 실제 사용하는 각 사이트의 콜백 URI를 등록합니다. 실제 요청의 `redirect_uri`와 정확히 일치해야 합니다.

| 제공자 | 콜백 URI |
|---|---|
| 네이버 | `사이트주소/login/oauth2/code/naver` |
| 카카오 | `사이트주소/login/oauth2/code/kakao` |
| 구글 | `https://실제도메인/login/oauth2/code/google` |

`frontendUrl`은 백엔드 로그인 처리 후 프론트엔드로 돌아갈 주소입니다. 제공자의 콜백 URI와 역할이 다릅니다.

배포 사이트의 구글 웹 로그인에는 도메인과 HTTPS가 필요합니다. 실제 요청의 콜백도 HTTPS인지 확인합니다. 테스트 모드라면 테스트 사용자도 등록합니다. [Google OAuth URI 규칙](https://developers.google.com/identity/protocols/oauth2/web-server#uri-validation)

## 오류 확인과 종료

- DB `Connect timed out`: VPN·BGP·AWS 경로 전파·RDS 보안 그룹 확인.
- `password authentication failed`: RDS 비밀번호와 `secret-values.yaml` 확인.
- `502`: 새 Pod 시작 로그·로드밸런서 헬스 체크 확인.
- 로그인 후 예제 도메인으로 이동: 해당 배포의 `frontendUrl` 수정.
- 예전 클러스터 DNS 오류: kubeconfig 갱신 및 현재 context 확인.

터미널을 닫아도 비용은 계속 발생합니다. 종료 시에는 **각 클러스터의 앱·Ingress와 실제 로드밸런서 삭제 완료 → VPN → AWS·GCP Terraform 인프라** 순서로 정리합니다. Terraform state와 배포 변수는 삭제가 끝날 때까지 보관하세요. 이 프로젝트는 S3·GCS 사진을 삭제하고 RDS 최종 스냅샷을 생략하도록 설정되어 있습니다.

마지막으로 디스크·스냅샷·NAT·외부 IP·이미지 저장소 등 잔여물을 확인하고, GCP 프로젝트를 사용하지 않는다면 결제 연결도 해제합니다. 이미 발생한 요금은 나중에 청구될 수 있습니다.
