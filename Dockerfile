# syntax=docker/dockerfile:1

# --- Stage 1: build frontend ---
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend

COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci

COPY frontend/ ./
RUN npm run build


# --- Stage 2: build backend jar (skip frontend gradle build; we copy dist in) ---
FROM gradle:8.10.2-jdk21 AS backend-build
WORKDIR /app

# Copy sources
COPY backend/ ./backend/
COPY frontend/ ./frontend/

# Inject built frontend assets so Spring Boot serves them
COPY --from=frontend-build /app/frontend/dist ./frontend/dist

WORKDIR /app/backend

# Build the executable jar; skip the buildFrontend task (it runs node)
RUN ./gradlew bootJar -x buildFrontend


# --- Stage 3: runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=backend-build /app/backend/build/libs/*.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
