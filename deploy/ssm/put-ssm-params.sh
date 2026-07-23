#!/usr/bin/env bash
# ============================================================================
# SSM Parameter Store 저장 스크립트 (prod 자격증명/설정)
# ----------------------------------------------------------------------------
# 값은 이 파일에 두지 않는다. 실행 전 아래 환경변수를 셸에 export 하고 실행:
#   DB_USER DB_PASSWORD OPENAI_API_KEY
#   GITHUB_OAUTH_CLIENT_ID GITHUB_OAUTH_CLIENT_SECRET GITHUB_OAUTH_REDIRECT_URI
#   JWT_SECRET
# 필요 권한: ssm:PutParameter (+ SecureString이면 kms:Encrypt)
# ============================================================================
set -euo pipefail

REGION="${AWS_REGION:-ap-northeast-2}"
PREFIX="/portfolio/prod"

command -v aws >/dev/null || { echo "ERROR: aws CLI 필요"; exit 1; }

# put <param-name> <String|SecureString> <ENV_VAR_NAME>
put() {
  local name="$1" type="$2" var="$3"
  local val="${!var:-}"
  if [ -z "$val" ]; then
    echo "SKIP  $PREFIX/$name  (env $var 미설정)"
    return
  fi
  aws ssm put-parameter \
    --region "$REGION" \
    --name "$PREFIX/$name" \
    --type "$type" \
    --value "$val" \
    --overwrite >/dev/null
  echo "OK    $PREFIX/$name  ($type)"
}

put DB_USER                    String       DB_USER
put DB_PASSWORD                SecureString DB_PASSWORD
put OPENAI_API_KEY             SecureString OPENAI_API_KEY
put GITHUB_OAUTH_CLIENT_ID     String       GITHUB_OAUTH_CLIENT_ID
put GITHUB_OAUTH_CLIENT_SECRET SecureString GITHUB_OAUTH_CLIENT_SECRET
put GITHUB_OAUTH_REDIRECT_URI  String       GITHUB_OAUTH_REDIRECT_URI
put JWT_SECRET                 SecureString JWT_SECRET

echo ""
echo "완료. 이름만 확인(값 노출 없음):"
echo "  aws ssm get-parameters-by-path --region $REGION --path $PREFIX --recursive --query 'Parameters[].Name' --output text"
