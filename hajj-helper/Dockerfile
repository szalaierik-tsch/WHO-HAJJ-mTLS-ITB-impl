# Stage 1: Build application
FROM maven:3.9.2-amazoncorretto-17 AS builder

WORKDIR /app
COPY . /app
RUN mvn clean install -DskipTests=true

# Stage 2: Run application
FROM eclipse-temurin:21-jre-jammy
RUN mkdir /app
COPY --from=builder /app/target/hajj-helper-services-1.0-SNAPSHOT.jar /app/app.jar
RUN sh -c 'touch /app/app.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Xmx2048m","-jar","/app/app.jar"]
EXPOSE 8181
WORKDIR /app