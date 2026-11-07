#!/usr/bin/env bash
set -euo pipefail
set +x

cloud="${1:?aws 또는 gcp 지정}"
action="${2:?publish 또는 deploy 지정}"
[[ "$cloud" == aws || "$cloud" == gcp ]] || exit 2
[[ "$action" == publish || "$action" == deploy ]] || exit 2
: "${IMAGE_TAG:?IMAGE_TAG 필요}"
[[ "$IMAGE_TAG" =~ ^[a-f0-9]{12}-[0-9]+$ ]] || { echo "이미지 태그 확인 필요"; exit 2; }

work_dir="$(mktemp -d /tmp/photo-storage-deploy.XXXXXXXX)"
trap 'rm -rf -- "$work_dir"' EXIT
export KUBECONFIG="$work_dir/kubeconfig"
export DOCKER_CONFIG="$work_dir/docker"
export CLOUDSDK_CONFIG="$work_dir/gcloud"
export AWS_PAGER=""
export AWS_PROFILE=default
mkdir -p "$DOCKER_CONFIG" "$CLOUDSDK_CONFIG"
namespace=photo-storage
release=photo-storage
context="photo-storage-$cloud"

if [[ "$cloud" == aws ]]; then
  : "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID 필요}" "${AWS_REGION:?AWS_REGION 필요}" "${AWS_SHARED_CREDENTIALS_FILE:?AWS 인증 파일 필요}"
  actual_account="$(aws sts get-caller-identity --query Account --output text)"
  [[ "$actual_account" == "$AWS_ACCOUNT_ID" ]] || { echo "AWS 계정 불일치"; exit 1; }
  aws eks update-kubeconfig --name photo-storage-eks --region "$AWS_REGION" \
    --kubeconfig "$KUBECONFIG" --alias "$context" >/dev/null
  registry="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
  frontend_repo="$registry/photo-storage-frontend"
  backend_repo="$registry/photo-storage-backend"
else
  : "${GCP_PROJECT_ID:?GCP_PROJECT_ID 필요}" "${GCP_REGION:?GCP_REGION 필요}" "${GCP_CREDENTIALS_FILE:?GCP 인증 파일 필요}"
  gcloud auth login --cred-file="$GCP_CREDENTIALS_FILE" --quiet >/dev/null
  gcloud container clusters get-credentials photo-storage-cluster \
    --region="$GCP_REGION" --project="$GCP_PROJECT_ID" --quiet >/dev/null
  kubectl config rename-context "gke_${GCP_PROJECT_ID}_${GCP_REGION}_photo-storage-cluster" "$context" >/dev/null
  registry="$GCP_REGION-docker.pkg.dev"
  frontend_repo="$registry/$GCP_PROJECT_ID/photo-storage/photo-storage-frontend"
  backend_repo="$registry/$GCP_PROJECT_ID/photo-storage/photo-storage-backend"
fi

# Initial infrastructure, app configuration and secrets are prepared separately.
helm status "$release" -n "$namespace" --kube-context "$context" >/dev/null
current_cloud="$(helm get values "$release" -n "$namespace" --kube-context "$context" -a -o json | python3 -c 'import json,sys; print(json.load(sys.stdin)["backend"]["env"]["cloudProvider"])')"
[[ "$current_cloud" == "$cloud" ]] || { echo "배포 대상 불일치"; exit 1; }

if [[ "$action" == publish ]]; then
  if [[ "$cloud" == aws ]]; then
    aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$registry"
  else
    gcloud auth print-access-token | docker login --username oauth2accesstoken --password-stdin "$registry"
  fi
  docker tag "photo-storage-frontend:$IMAGE_TAG" "$frontend_repo:$IMAGE_TAG"
  docker tag "photo-storage-backend:$IMAGE_TAG" "$backend_repo:$IMAGE_TAG"
  docker push "$frontend_repo:$IMAGE_TAG"
  docker push "$backend_repo:$IMAGE_TAG"
  docker image rm "$frontend_repo:$IMAGE_TAG" "$backend_repo:$IMAGE_TAG" >/dev/null
else
  helm upgrade "$release" infra/helm/photo-storage \
    -n "$namespace" --kube-context "$context" --reuse-values \
    --set-string frontend.image.repository="$frontend_repo" \
    --set-string backend.image.repository="$backend_repo" \
    --set-string frontend.image.tag="$IMAGE_TAG" \
    --set-string backend.image.tag="$IMAGE_TAG" \
    --set backend.probes.enabled=true \
    --atomic --wait --timeout 10m
  kubectl --context "$context" rollout status deployment/photo-storage-backend -n "$namespace" --timeout=120s
  kubectl --context "$context" rollout status deployment/photo-storage-frontend -n "$namespace" --timeout=120s
fi
