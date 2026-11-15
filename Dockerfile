FROM node:22-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src src
COPY --from=web /web/dist src/main/resources/static
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
COPY --from=build /app/target/headerscheck-0.1.0.jar /app.jar
USER nobody
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
