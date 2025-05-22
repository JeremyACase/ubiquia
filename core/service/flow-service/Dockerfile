FROM openjdk:17

ENV PORT=8080
ENV JAR_FILE=ubiquia-core-flow-service.jar
ENV TARGET_DIR=/app

COPY build/libs/ubiquia-core-flow-service.jar ${TARGET_DIR}/${JAR_FILE}

EXPOSE ${PORT}

CMD ["java", "-Dspring.config.location=file:/app/etc/application.yaml", "-jar", "/app/ubiquia-core-flow-service.jar"]