FROM gradle:8-jdk17 AS build
WORKDIR /build
COPY poc-service-request/build.gradle .
COPY poc-service-request/src ./src
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
