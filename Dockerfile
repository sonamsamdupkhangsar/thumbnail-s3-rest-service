FROM maven:3-openjdk-17-slim as build

WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN ["mvn", "clean", "install"]

FROM openjdk:17
WORKDIR /app
COPY --from=build /app/target/thumbnail-s3-rest-service-1.0-SNAPSHOT.jar /app/thumbnail-s3-rest-service.jar
EXPOSE 8080

ENTRYPOINT [ "java", "-jar", "/app/thumbnail-s3-rest-service.jar"]

LABEL org.opencontainers.image.source https://github.com/sonamsamdupkhangsar/thumbnail-s3-rest-service