FROM eclipse-temurin:21-jre

# User non-root dengan UID tetap (sama dengan securityContext di Kubernetes)
RUN groupadd -r -g 10001 app \
 && useradd -r -u 10001 -g app app \
 && mkdir -p /opt/koperasi/uploads/bukti-bayar \
 && chown -R app:app /opt/koperasi

WORKDIR /app
COPY target/*.jar app.jar

USER app
EXPOSE 8080

# Heap mengikuti batas memori container
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["java", "-jar", "app.jar"]
