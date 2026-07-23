# gh-backend-deploy IAM (GitHub Actions → SSM 배포용)

GitHub Actions가 OIDC로 이 역할을 assume해서 `ssm:SendCommand` 로 EC2에 배포한다.
아래 JSON은 콘솔에 그대로 붙여넣을 수 있는 **순수 IAM 정책**이다(주석/추가 키 없음 — IAM은
`Version`/`Statement` 외 최상위 키를 거부한다).

## 선행: GitHub OIDC 공급자
IAM → Identity providers 에 아래가 없으면 먼저 추가:
- Provider URL: `https://token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`

## 역할 생성
1. `gh-backend-deploy` 역할 생성, **신뢰 정책** = `gh-backend-deploy-trust.json`
   - sub 조건 `repo:u1000e/KH_Portfolio_Builder:ref:refs/heads/main` (레포 실제 대소문자 유지)
   - `ForAllValues:` 미사용, `StringEquals` 로 정확 지정(값 부재/오타 통과 취약점 방지)
2. **권한 정책**(인라인 또는 관리형) = `gh-backend-deploy-permissions.json`
   - `ssm:SendCommand` → `AWS-RunShellScript` 문서 + 인스턴스 `i-0cda2a7683012f57c` 로 한정
   - `ssm:GetCommandInvocation` / `ListCommandInvocations` → 결과 확인용(리소스 수준 제한 미지원이라 `*`)
3. 워크플로의 `role-to-assume` = `arn:aws:iam::154667447418:role/gh-backend-deploy`

## 참고
- 앱 시크릿/S3 접근은 이 역할이 아니라 **EC2 인스턴스 역할(ec2-app-role)** 담당(브리프 §0).
- 인스턴스가 바뀌면 permission JSON의 instance ARN과 워크플로 `INSTANCE_ID` 를 함께 수정.
