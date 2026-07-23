#!/usr/bin/env bash
# ============================================================================
# SSM → .env 생성 (EC2 기동 시, 인스턴스 역할로 실행 — Phase 2)
# ----------------------------------------------------------------------------
# /portfolio/prod 하위 파라미터를 복호화해 KEY=VALUE 형태의 .env 로 떨군다.
# docker-compose 의 env_file 로 소비. 결과 파일은 평문이므로 chmod 600.
# 필요 권한(인스턴스 역할): ssm:GetParametersByPath (+ kms:Decrypt)
# 사용: ./fetch-ssm-env.sh [출력경로(기본 /srv/app/.env)]
# ============================================================================
set -euo pipefail

REGION="${AWS_REGION:-ap-northeast-2}"
PREFIX="/portfolio/prod"
OUT="${1:-/srv/app/.env}"

command -v aws >/dev/null || { echo "ERROR: aws CLI 필요"; exit 1; }

umask 077
TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

aws ssm get-parameters-by-path \
  --region "$REGION" \
  --path "$PREFIX" \
  --recursive \
  --with-decryption \
  --query "Parameters[].[Name,Value]" \
  --output text \
| while IFS=$'\t' read -r name value; do
    printf '%s=%s\n' "${name##*/}" "$value"
  done > "$TMP"

if [ ! -s "$TMP" ]; then
  echo "ERROR: 파라미터 0건. 경로/권한 확인: $PREFIX"
  exit 1
fi

mv "$TMP" "$OUT"
trap - EXIT
chmod 600 "$OUT"
echo "wrote $OUT (chmod 600, $(wc -l < "$OUT") vars)"
echo "키 목록(값 제외): $(cut -d= -f1 "$OUT" | tr '\n' ' ')"
