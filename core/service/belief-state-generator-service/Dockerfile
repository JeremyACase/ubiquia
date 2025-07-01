ARG OPENJDK_VERSION
FROM eclipse-temurin:${OPENJDK_VERSION}

ENV PORT=8080
ENV JAR_FILE=ubiquia-core-belief-state-generator-service.jar
ENV TARGET_DIR=/app

COPY build/libs/ubiquia-core-belief-state-generator-service.jar ${TARGET_DIR}/${JAR_FILE}

EXPOSE ${PORT}

CMD ["java", "-Dspring.config.location=file:/app/etc/application.yaml", "-jar", "/app/ubiquia-core-belief-state-generator-service.jar"]