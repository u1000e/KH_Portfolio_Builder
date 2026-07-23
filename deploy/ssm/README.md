# SSM Parameter Store 준비 (Phase 2 선행)

운영(prod) 자격증명·설정을 **AWS SSM Parameter Store(SecureString)** 에 저장하고,
EC2에서 컨테이너 기동 시 환경변수로 주입한다. **실제 값은 어떤 파일에도 쓰지 않는다** —
운영자가 셸 env로 넣고 스크립트를 돌린다.

- 리전: `ap-northeast-2`
- 파라미터 경로(prefix): `/portfolio/prod`
- 암호화: SecureString = KMS. 기본 키 `alias/aws/ssm` 사용(전용 CMT를 쓰려면 put 시 `--key-id`).
- **로컬 개발은 SSM 미사용** — 로컬은 로컬 .env/셸 env + 로컬 OAuth App + SSM 터널 DB. SSM은 prod 전용.

## 1. 파라미터 매핑 (앱 env ↔ SSM ↔ 타입)

| 앱 환경변수 | SSM 파라미터 | 타입 | 비고 |
|---|---|---|---|
| `DB_USER` | `/portfolio/prod/DB_USER` | String | app_user (식별자) |
| `DB_PASSWORD` | `/portfolio/prod/DB_PASSWORD` | **SecureString** | RDS app_user 비번 |
| `OPENAI_API_KEY` | `/portfolio/prod/OPENAI_API_KEY` | **SecureString** | |
| `GITHUB_OAUTH_CLIENT_ID` | `/portfolio/prod/GITHUB_OAUTH_CLIENT_ID` | String | prod OAuth App |
| `GITHUB_OAUTH_CLIENT_SECRET` | `/portfolio/prod/GITHUB_OAUTH_CLIENT_SECRET` | **SecureString** | |
| `GITHUB_OAUTH_REDIRECT_URI` | `/portfolio/prod/GITHUB_OAUTH_REDIRECT_URI` | String | 예: https://kh-jongno.shop/auth/callback |
| `JWT_SECRET` | `/portfolio/prod/JWT_SECRET` | **SecureString** | Base64 32바이트↑ |

- **AWS 자격증명(access/secret key)은 SSM에 넣지 않는다.** 앱 S3 클라이언트는 EC2 인스턴스
  역할(DefaultCredentialsProvider)로 획득한다(이미 코드 반영됨).
- `REDIS_HOST`/`REDIS_PORT`, `SPRING_PROFILES_ACTIVE=prod` 등 비민감/운영값은 SSM 아닌
  docker-compose에 둔다.
- prod OAuth App은 로컬과 **다른 앱**(분리 유지 결정) — SSM에는 prod 값만.

## 2. 저장 (한 번, 값 회전 시 재실행)

`put-ssm-params.sh` 는 값을 파일에 두지 않고 **셸 env에서 읽어** put 한다.
운영자가 회전된 실제 값을 env로 설정한 뒤 실행:

```bash
export AWS_REGION=ap-northeast-2
export DB_USER=... DB_PASSWORD=... OPENAI_API_KEY=...
export GITHUB_OAUTH_CLIENT_ID=... GITHUB_OAUTH_CLIENT_SECRET=...
export GITHUB_OAUTH_REDIRECT_URI=https://kh-jongno.shop/auth/callback
export JWT_SECRET=...
./put-ssm-params.sh
# 확인(값 없이 이름만):
aws ssm get-parameters-by-path --path /portfolio/prod --recursive --query 'Parameters[].Name'
```

> put 하는 머신에는 SSM 쓰기 권한(`ssm:PutParameter`, SecureString이면 `kms:Encrypt`)이
> 필요하다. 개인 관리 계정/프로파일로 실행.

## 3. EC2에서 주입 (Phase 2 기동 시)

`fetch-ssm-env.sh` 가 경로의 파라미터를 복호화해 `.env`(chmod 600)로 떨군다.
docker-compose 가 `env_file` 로 읽는다.

```bash
# EC2에서 (인스턴스 역할로 실행)
./fetch-ssm-env.sh /srv/app/.env
# docker-compose.yml (Phase 2):
#   services:
#     api:
#       env_file: [/srv/app/.env]
#       environment: [ "SPRING_PROFILES_ACTIVE=prod", "REDIS_HOST=redis" ]
```

### EC2 인스턴스 역할 IAM 정책(읽기 전용, 최소권한)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    { "Effect": "Allow",
      "Action": ["ssm:GetParametersByPath","ssm:GetParameter","ssm:GetParameters"],
      "Resource": "arn:aws:ssm:ap-northeast-2:<ACCOUNT_ID>:parameter/portfolio/prod/*" },
    { "Effect": "Allow",
      "Action": ["kms:Decrypt"],
      "Resource": "arn:aws:kms:ap-northeast-2:<ACCOUNT_ID>:key/<SSM_KMS_KEY_ID>" }
  ]
}
```

- 기본 키(`alias/aws/ssm`)를 쓰면 `kms:Decrypt` 대상은 그 aws 관리형 키. 전용 CMK면 해당 키 ARN.
- 이 역할은 **읽기 전용**. put 권한은 부여하지 않는다.

## 4. 주의

- `.env`(fetch 결과)에는 평문 값이 담긴다 → chmod 600, git 미추적(배포 디렉터리에만).
- 값 회전 시: SSM 재put → EC2에서 fetch 재실행 → `docker compose up -d --no-deps api` 로 반영.
- 값에 개행이 있으면 .env 포맷이 깨진다. 대상 값(비번/키/base64/hex/URL)은 개행 없음 — 안전.
