# === PC Frontend Build Stage ===
FROM node:22-alpine AS frontend-build

WORKDIR /app/front

COPY front/package.json front/package-lock.json* ./
RUN npm ci

COPY front/ ./
# Vite 빌드 출력 경로를 Docker 빌드 컨텍스트용으로 오버라이드
RUN npx vite build --outDir /app/static --emptyOutDir

# === Mobile Frontend Build Stage ===
FROM node:22-alpine AS frontend-mobile-build

WORKDIR /app/front-mobile

COPY front-mobile/package.json front-mobile/package-lock.json* ./
RUN npm ci

COPY front-mobile/ ./
# 모바일 Vite 빌드 출력
RUN npx vite build --outDir /app/static-mobile --emptyOutDir

# === Backend Build Stage ===
FROM eclipse-temurin:25-jdk-alpine AS backend-build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src src

# PC 프론트엔드 빌드 결과물을 Spring Boot static 리소스 디렉토리로 복사
COPY --from=frontend-build /app/static/ src/main/resources/static/
# 모바일 프론트엔드 빌드 결과물을 Spring Boot static-mobile 리소스 디렉토리로 복사
COPY --from=frontend-mobile-build /app/static-mobile/ src/main/resources/static-mobile/

RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

# === Run Stage ===
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=backend-build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

