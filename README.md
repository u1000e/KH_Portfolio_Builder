# Portfolio Builder API

포트폴리오 빌더 백엔드 서버

## 기술 스택

- Java 21
- Spring Boot 3.5
- Oracle 18c
- AWS S3

## 로컬 실행

### 1. 설정 파일 생성

`src/main/resources/application.yml` 파일 생성 후 아래 내용 작성:

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:oracle:thin:@<DB_HOST>:<DB_PORT>:XE
    username: <DB_USER>
    password: <DB_PASSWORD>
    driver-class-name: oracle.jdbc.driver.OracleDriver

  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true

  security:
    oauth2:
      client:
        registration:
          github:
            client-id: <GITHUB_CLIENT_ID>
            client-secret: <GITHUB_CLIENT_SECRET>
            scope: user:email,read:user,repo

jwt:
  secret: <JWT_SECRET_KEY_64자_이상>
  expiration: 86400000

app:
  frontend:
    url: http://localhost:5173

github:
  client:
    id: <GITHUB_CLIENT_ID>
    secret: <GITHUB_CLIENT_SECRET>
  redirect:
    uri: http://localhost:5173/auth/callback

aws:
  access-key: <AWS_ACCESS_KEY>
  secret-key: <AWS_SECRET_KEY>
  region: ap-northeast-2
  s3:
    bucket: <S3_BUCKET_NAME>

# Spring AI - OpenAI 설정
spring.ai:
  openai:
    api-key: ${OPENAI_API_KEY}
    chat:
      options:
        model: gpt-4o-mini
        temperature: 0.3
```

### 2. 실행

```bash
./gradlew bootRun
```

## 배포

프로덕션 환경에서는 `application-prod.yml` 사용

```bash
./gradlew bootJar
java -jar build/libs/portfolio-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## API 문서

| Method | Endpoint                  | 설명               |
| ------ | ------------------------- | ------------------ |
| GET    | /health                   | 헬스체크           |
| GET    | /api/auth/github/login    | GitHub 로그인 URL  |
| GET    | /api/auth/github/callback | OAuth 콜백         |
| GET    | /api/portfolios           | 내 포트폴리오 목록 |
| POST   | /api/portfolios           | 포트폴리오 생성    |
| GET    | /api/public/portfolios    | 공개 포트폴리오    |
