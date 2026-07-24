#!/usr/bin/env bash
# /srv/app/deploy.sh — SSM Run Command가 root로 호출. api 컨테이너만 무중단에 가깝게 교체.
# redis/caddy 는 건드리지 않는다(--no-deps).
set -euo pipefail

REGION="ap-northeast-2"
PREFIX="/portfolio/prod"
APP_DIR="/srv/app"
ENV_FILE="$APP_DIR/.env"
ENV_ALLOY="$APP_DIR/.env.alloy"
cd "$APP_DIR"

echo "[1/6] SSM에서 .env / .env.alloy 생성 (앱 시크릿과 모니터링 자격증명 분리; GHCR_* 는 둘 다 제외)"
umask 077
# 두 파일을 먼저 비운다 — 파라미터가 하나도 없어도 compose가 env_file 파싱에 실패하지 않도록 존재 보장.
: > "$ENV_FILE"
: > "$ENV_ALLOY"
aws ssm get-parameters-by-path --region "$REGION" --path "$PREFIX" --recursive --with-decryption \
  --query "Parameters[].[Name,Value]" --output text \
  | while IFS=$'\t' read -r name value; do
      key="${name##*/}"
      case "$key" in
        GHCR_*)          continue ;;                                      # GHCR 자격증명은 어느 앱 env에도 안 넣음(login은 stdin 별도)
        GRAFANA_CLOUD_*) printf '%s=%s\n' "$key" "$value" >> "$ENV_ALLOY" ;;  # alloy 전용 — 앱은 알 필요 없음
        *)               printf '%s=%s\n' "$key" "$value" >> "$ENV_FILE" ;;   # api 앱 env
      esac
    done
chmod 600 "$ENV_FILE" "$ENV_ALLOY"
# 평문 시크릿이 남으므로 600 필수(소유자 root). 컨테이너 재시작 시 필요해서 유지한다
# (브리프 §3-4: run 시점에만 생성/삭제하는 방식보다 실용적이라는 판단).
echo "  .env keys:       $(cut -d= -f1 "$ENV_FILE" | tr '\n' ' ')"
echo "  .env.alloy keys: $(cut -d= -f1 "$ENV_ALLOY" | tr '\n' ' ')"

echo "[2/6] GHCR 로그인 (PAT는 SSM에서 직접 읽어 stdin으로 — .env/디스크에 안 남김)"
GHCR_USERNAME=$(aws ssm get-parameter --region "$REGION" --name "$PREFIX/GHCR_USERNAME" --query Parameter.Value --output text)
aws ssm get-parameter --region "$REGION" --name "$PREFIX/GHCR_PAT" --with-decryption --query Parameter.Value --output text \
  | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin

echo "[3/6] api 이미지 pull"
docker compose pull api

echo "[4/6] api 컨테이너만 교체 (redis/caddy 유지)"
docker compose up -d --no-deps api

echo "[5/6] 미사용 이미지 정리 (디스크 누적 방지)"
docker image prune -f

echo "[6/6] 헬스체크: /health 200, 최대 60초 폴링"
API_CID="$(docker compose ps -q api)"
NET="$(docker inspect -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}' "$API_CID")"
ok=0
for i in $(seq 1 12); do
  if docker run --rm --network "$NET" curlimages/curl:latest -fsS "http://api:8081/health" >/dev/null 2>&1; then
    ok=1; break
  fi
  sleep 5
done
if [ "$ok" != 1 ]; then
  echo "헬스체크 실패 — api 최근 로그:"
  docker compose logs --tail=50 api
  exit 1
fi
echo "배포 성공."
docker compose ps
