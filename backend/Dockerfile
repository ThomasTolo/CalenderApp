# syntax=docker/dockerfile:1

FROM gradle:8.10.2-jdk21 AS build
WORKDIR /app

COPY . ./

# Build the executable jar; skip frontend gradle task (frontend runs as its own container)
RUN ./gradlew bootJar -x buildFrontend

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
