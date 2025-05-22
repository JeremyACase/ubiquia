FROM openjdk:17

ENV PORT=8080

ENV JAR_FILE=build/libs/amigos-4.5.0.jar

COPY ${JAR_FILE}        /app/app.jar

EXPOSE ${PORT}

COPY src/main/resources/bootstrap/ontologies/    /app/ontologies/

CMD [ "java", "-Dspring.config.location=file:/app/etc/application.yaml", "-jar", "/app/app.jar" ]