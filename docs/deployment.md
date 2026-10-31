# 클라우드 배포 실행

예제 파일을 복사한 후 데이터베이스 비밀번호와 OAuth Client 정보를 입력합니다.

```bash
cp infra/helm/photo-storage/secret-values.example.yaml \
  infra/helm/photo-storage/secret-values.yaml
```

```yaml
backend:
  env:
    datasourcePassword: 실제_DB_비밀번호

  oauth:
    google:
      clientId: 실제_GOOGLE_CLIENT_ID
      clientSecret: 실제_GOOGLE_CLIENT_SECRET
    naver:
      clientId: 실제_NAVER_CLIENT_ID
      clientSecret: 실제_NAVER_CLIENT_SECRET
    kakao:
      clientId: 실제_KAKAO_CLIENT_ID
```

다음 Terraform 명령은 각 디렉터리로 이동하기 전 프로젝트 루트에서 실행합니다.

### AWS
```bash
cd infra/terraform/aws
terraform init
terraform plan
terraform apply
```

### GCP
```bash
cd infra/terraform/gcp
terraform init
terraform plan
terraform apply
```

### VPN (AWS <-> GCP)
```bash
cd infra/terraform/multicloud-network
terraform init
terraform plan
terraform apply
```

### Docker image push
프로젝트 루트에서 프론트엔드와 백엔드 이미지를 빌드합니다.
```bash
docker build -t photo-storage-frontend:latest ./frontend
docker build -t photo-storage-backend:latest ./backend
```

AWS ECR에 로그인합니다.

```bash
AWS_REGION=ap-northeast-2
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

aws ecr get-login-password \
  --region $AWS_REGION \
  | docker login \
      --username AWS \
      --password-stdin \
      $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
```

ECR에 이미지를 push합니다.
```bash
docker tag photo-storage-frontend:latest \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/photo-storage-frontend:latest

docker tag photo-storage-backend:latest \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/photo-storage-backend:latest

docker push \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/photo-storage-frontend:latest

docker push \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/photo-storage-backend:latest
```

Google Artifact Registry에 로그인합니다.
```bash
GCP_REGION=asia-northeast3
GCP_PROJECT_ID=$(gcloud config get-value project)

gcloud auth configure-docker \
  $GCP_REGION-docker.pkg.dev
```

Artifact Registry에 이미지를 Push합니다.

```bash
docker tag photo-storage-frontend:latest \
  $GCP_REGION-docker.pkg.dev/$GCP_PROJECT_ID/photo-storage/photo-storage-frontend:latest

docker tag photo-storage-backend:latest \
  $GCP_REGION-docker.pkg.dev/$GCP_PROJECT_ID/photo-storage/photo-storage-backend:latest

docker push \
  $GCP_REGION-docker.pkg.dev/$GCP_PROJECT_ID/photo-storage/photo-storage-frontend:latest

docker push \
  $GCP_REGION-docker.pkg.dev/$GCP_PROJECT_ID/photo-storage/photo-storage-backend:latest
```


### EKS 배포
```bash
aws eks update-kubeconfig \
  --region ap-northeast-2 \
  --name photo-storage-eks

helm upgrade --install photo-storage \
  infra/helm/photo-storage \
  -f infra/helm/photo-storage/values-aws.yaml \
  -f infra/helm/photo-storage/secret-values.yaml
```

### GKE 배포
```bash
gcloud container clusters get-credentials photo-storage-cluster \
  --region asia-northeast3 \
  --project $GCP_PROJECT_ID

helm upgrade --install photo-storage \
  infra/helm/photo-storage \
  -f infra/helm/photo-storage/values-gcp.yaml \
  -f infra/helm/photo-storage/secret-values.yaml
```

### 배포 확인
```bash
kubectl get pods
kubectl get services
kubectl get ingress
```
