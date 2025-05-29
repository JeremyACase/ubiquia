FROM openjdk:17

ENV PORT=8080
ENV JAR_FILE=ubiquia-core-flow-service.jar
ENV TARGET_DIR=/app

# Create necessary directories for our graphs and agent communication languages (ACL's)
RUN mkdir -p ${TARGET_DIR}/etc/graphs ${TARGET_DIR}/etc/acls

COPY build/libs/ubiquia-core-flow-service.jar ${TARGET_DIR}/${JAR_FILE}

EXPOSE ${PORT}

CMD ["java", "-Dspring.config.location=file:/app/etc/application.yaml", "-jar", "/app/ubiquia-core-flow-service.jar"]