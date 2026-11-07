#!/usr/bin/env bash
set -euo pipefail

agent_name="${JENKINS_AGENT_NAME:-photo-storage-ci}"
controller_name="${JENKINS_CONTROLLER_CONTAINER:-photo-storage-jenkins}"
network_name="photo-storage-ci-net"
image_name="photo-storage-ci-agent:local"
container_name="photo-storage-ci-agent"
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

docker inspect "$controller_name" >/dev/null
if docker container inspect "$container_name" >/dev/null 2>&1; then
  echo "Agent container already exists. Start it with: docker start $container_name"
  exit 1
fi

docker build -t "$image_name" -f "$script_dir/Dockerfile.agent" "$script_dir"
docker network inspect "$network_name" >/dev/null 2>&1 || docker network create "$network_name"
if ! docker network inspect "$network_name" --format '{{range .Containers}}{{println .Name}}{{end}}' | grep -Fxq "$controller_name"; then
  docker network connect "$network_name" "$controller_name"
fi

read -rsp "Jenkins 노드 화면의 Secret 입력 (화면에 표시되지 않음): " JENKINS_SECRET
printf '\n'
if [[ -z "$JENKINS_SECRET" ]]; then
  echo "Secret이 비어 있습니다."
  exit 1
fi
export JENKINS_SECRET
trap 'unset JENKINS_SECRET' EXIT

docker run -d --init \
  --name "$container_name" \
  --restart unless-stopped \
  --network "$network_name" \
  -e JENKINS_SECRET \
  -e JENKINS_URL="http://$controller_name:8080/" \
  -e JENKINS_AGENT_NAME="$agent_name" \
  -e JENKINS_AGENT_WORKDIR=/home/jenkins/agent \
  -e JENKINS_WEB_SOCKET=true \
  -v photo-storage-ci-agent-work:/home/jenkins/agent \
  "$image_name"

echo "Jenkins의 노드가 온라인인지 확인하세요. 연결 로그: docker logs --tail 40 $container_name"
