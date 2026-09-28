FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S java && adduser -S java -G java
WORKDIR /app
COPY --from=builder --chown=java:java /build/target/ledger-service*.jar /app/application.jar
USER java
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/application.jar"]