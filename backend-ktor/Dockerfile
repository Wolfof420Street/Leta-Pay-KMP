FROM eclipse-temurin:21-jre-alpine

WORKDIR /opt/leta-pay

RUN apk add --no-cache curl \
    && addgroup -S letapay \
    && adduser -S letapay -G letapay

COPY backend-ktor/build/libs/*-all.jar app.jar

RUN chown -R letapay:letapay /opt/leta-pay

USER letapay

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/opt/leta-pay/app.jar"]
