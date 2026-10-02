# Build stage
FROM maven:3.9.7-eclipse-temurin-22 AS build

# spring-pg-multitenancy is not on Maven Central yet, install it into the local repository first
ARG MULTITENANCY_REF=main
ADD https://github.com/dvdmchl/spring-pg-multitenancy.git#${MULTITENANCY_REF} /multitenancy
RUN mvn -q -f /multitenancy/pom.xml install -DskipTests

WORKDIR /build
COPY . .
RUN mvn -q -pl code/backend -am package -DskipTests

# Runtime stage
FROM eclipse-temurin:22-jre
WORKDIR /app
COPY --from=build /build/code/backend/target/backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
