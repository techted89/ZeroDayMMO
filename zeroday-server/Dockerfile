# syntax=docker/dockerfile:1.6

# ---- Stage 1: build the fat JAR ----
FROM gradle:8.5-jdk17-alpine AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle build.gradle.kts settings.gradle.kts ./
COPY --chown=gradle:gradle src ./src
RUN gradle clean jar --no-daemon --console=plain

# ---- Stage 2: minimal runtime image ----
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="ZeroDayMMO" \
      description="ZeroDayMMO WebSocket game server" \
      version="1.0.0"

RUN apk add --no-cache tini curl \
    && addgroup -S zeroday && adduser -S zeroday -G zeroday \
    && mkdir -p /opt/zeroday/data /opt/zeroday/logs \
    && chown -R zeroday:zeroday /opt/zeroday

WORKDIR /opt/zeroday
COPY --from=build /home/gradle/src/build/libs/zeroday-server.jar /opt/zeroday/zeroday-server.jar
COPY --chown=zeroday:zeroday logback.xml /opt/zeroday/logback.xml

USER zeroday:zeroday
VOLUME ["/opt/zeroday/data", "/opt/zeroday/logs"]

ENV ZERODAY_PORT=8080 \
    ZERODAY_DATA_DIR=/opt/zeroday/data \
    ZERODAY_SERVER_NAME=ZeroDayMMO \
    JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD curl -fsS http://localhost:${ZERODAY_PORT}/health || exit 1

ENTRYPOINT ["/sbin/tini", "--"]
CMD ["sh", "-c", "exec java $JAVA_OPTS -Dlogback.configurationFile=/opt/zeroday/logback.xml -jar /opt/zeroday/zeroday-server.jar"]
