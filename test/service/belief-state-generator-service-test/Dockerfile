ARG OPENJDK_VERSION
FROM eclipse-temurin:${OPENJDK_VERSION}

ENV PORT=8080
ENV JAR_FILE=ubiquia-test-belief-state-generator-service.jar
ENV TARGET_DIR=/app

COPY build/libs/ubiquia-test-belief-state-generator-service.jar ${TARGET_DIR}/${JAR_FILE}
COPY build/src/main/resources/pets-acl-json-schema.json ${TARGET_DIR}/etc/pets-acl-json-schema.json

EXPOSE ${PORT}

CMD ["java", "-Dspring.config.location=file:/app/etc/application.yaml", "-jar", "/app/ubiquia-test-belief-state-generator-service.jar"]