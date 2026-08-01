FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -B -DskipTests -Djavafx.platform=linux

FROM alpine:3.19 AS runner
RUN apk add --no-cache openjdk25-jre

COPY --from=builder /app/target/*.jar /app/ultimate-crypto-suite.jar

EXPOSE 8080
CMD ["java", "-jar", "/app/ultimate-crypto-suite.jar"]
