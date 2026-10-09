FROM maven:3.9.11-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress verify

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S flowpay && adduser -S flowpay -G flowpay
WORKDIR /app
COPY --from=build /workspace/target/flowpay-1.0.0.jar app.jar
ENV SERVER_ADDRESS=0.0.0.0
USER flowpay
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

