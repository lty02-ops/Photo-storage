#!/usr/bin/env bash
set -euo pipefail

role="${AGENT_ROLE:-ci}"
case "$role" in
  ci) default_name=photo-storage-ci; dockerfile=Dockerfile.agent ;;
  deploy) default_name=photo-storage-deploy; dockerfile=Dockerfile.deploy ;;
  *) echo "AGENT_ROLE: ci 또는 deploy"; exit 2 ;;
esac
agent_name="${JENKINS_AGENT_NAME:-$default_name}"
controller_name="${JENKINS_CONTROLLER_CONTAINER:-photo-storage-jenkins}"
network_name="photo-storage-ci-net"
image_name="$default_name-agent:local"
container_name="$default_name-agent"
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

docker inspect "$controller_name" >/dev/null
if docker container inspect "$container_name" >/dev/null 2>&1; then
  echo "Agent container already exists. Start it with: docker start $container_name"
  exit 1
fi

run_args=()
if [[ "$role" == deploy ]]; then
  docker build -t photo-storage-ci-agent:local -f "$script_dir/Dockerfile.agent" "$script_dir"
  socket=/var/run/docker.sock
  [[ -S "$socket" ]] || { echo "Docker 소켓 확인 필요: $socket"; exit 1; }
  run_args+=(--mount "type=bind,source=$socket,target=$socket" --group-add "$(stat -c '%g' "$socket")")
fi
docker build -t "$image_name" -f "$script_dir/$dockerfile" "$script_dir"
docker network inspect "$network_name" >/dev/null 2>&1 || docker network create "$network_name"
if ! docker network inspect "$network_name" --format '{{range .Containers}}{{println .Name}}{{end}}' | grep -Fxq "$controller_name"; then
  docker network connect "$network_name" "$controller_name"
fi

read -rsp "에이전트 Secret: " JENKINS_SECRET
printf '\n'
if [[ -z "$JENKINS_SECRET" ]]; then
  echo "Secret을 입력하세요."
  exit 1
fi
export JENKINS_SECRET
trap 'unset JENKINS_SECRET' EXIT

docker run -d --init \
  --name "$container_name" \
  --restart unless-stopped \
  --network "$network_name" \
  "${run_args[@]}" \
  -e JENKINS_SECRET \
  -e JENKINS_URL="http://$controller_name:8080/" \
  -e JENKINS_AGENT_NAME="$agent_name" \
  -e JENKINS_AGENT_WORKDIR=/home/jenkins/agent \
  -e JENKINS_WEB_SOCKET=true \
  -v "$container_name-work:/home/jenkins/agent" \
  "$image_name"

echo "연결 로그: docker logs --tail 40 $container_name"
