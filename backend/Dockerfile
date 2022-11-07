FROM openjdk:17-jdk-slim
COPY build/libs/backend-1.0.0-SNAPSHOT.jar app.jar
ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
