# openjdk:17 공식 이미지는 Docker Hub 에서 사라져(manifest unknown) 빌드가 실패하므로, 유지 관리되는 Temurin JRE 를 쓴다.
FROM eclipse-temurin:17-jre
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]
