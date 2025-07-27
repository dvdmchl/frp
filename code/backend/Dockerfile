# Build stage
FROM maven:3.9.7-eclipse-temurin-22 AS build
WORKDIR /build
COPY . .
RUN mvn -q -pl code/backend -am package

# Runtime stage
FROM eclipse-temurin:22-jre
WORKDIR /app
COPY --from=build /build/code/backend/target/backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
