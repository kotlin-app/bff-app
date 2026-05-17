# Stage 1: ビルド
FROM gradle:8.10.2-jdk21 AS builder
WORKDIR /app

# 依存関係のキャッシュを活かすため、先にビルドファイルだけコピー
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# ソースコードをコピーしてビルド
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# Stage 2: 実行
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
