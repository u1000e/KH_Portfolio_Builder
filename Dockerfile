# 단일 스테이지 — jar는 GitHub Actions 러너(x86)에서 네이티브 빌드하고 여기선 COPY만.
# Java 바이트코드는 아키텍처 독립이므로, buildx --platform linux/arm64 로 빌드해도
# QEMU 에뮬레이션 부담이 없다(COPY뿐).
FROM eclipse-temurin:21-jre
WORKDIR /app

# 비루트 실행
RUN useradd -r -u 1001 appuser

# 러너에서 만든 실행 가능 jar 하나만 복사 (워크플로가 *-plain.jar 는 제거함)
COPY build/libs/*.jar app.jar

USER appuser
EXPOSE 8081

# MaxRAMPercentage=60: 2GB 인스턴스에서 컨테이너 메모리 대비 힙 상한 명시
# (미지정 시 기본 25%라 과소, 상한 없으면 과다 → OOM 위험)
ENTRYPOINT ["java","-XX:MaxRAMPercentage=60","-jar","app.jar"]
