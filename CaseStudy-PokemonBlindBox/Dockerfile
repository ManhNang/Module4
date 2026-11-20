# ==============================================================================
# Multi-Stage Dockerfile cho dự án Pokémon Blindbox (Spring Boot 3 + Thymeleaf)
# Stage 1: Build với Maven 3.9 & Eclipse Temurin JDK 21 Alpine
# Stage 2: Runtime tối ưu với Eclipse Temurin JRE 21 Alpine (Non-root user)
# ==============================================================================

# -------------------- STAGE 1: MAVEN BUILD --------------------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

# Tối ưu Docker Layer Caching: tải trước dependencies khi pom.xml không đổi
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy mã nguồn Java, Flyway SQL Migrations và toàn bộ Thymeleaf Templates
COPY src ./src
RUN mvn clean package -DskipTests -B

# -------------------- STAGE 2: PRODUCTION RUNTIME --------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Cài đặt wget & tzdata phục vụ Healthcheck và múi giờ Asia/Ho_Chi_Minh
RUN apk add --no-cache wget tzdata && \
    addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

ENV TZ=Asia/Ho_Chi_Minh
WORKDIR /app

# Copy file JAR đã đóng gói (chứa sẵn toàn bộ templates Thymeleaf) từ Stage 1
COPY --from=build --chown=appuser:appgroup /workspace/target/*.jar /app/app.jar

USER appuser
EXPOSE 8080

HEALTHCHECK --interval=20s --timeout=5s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
