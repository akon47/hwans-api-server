FROM openjdk:17
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-Dspring.config.location=classpath:/application.yml,/secret/application-secret.yml","-jar", "/app.jar"]
