FROM gradle:7.4.2-jdk11 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"] 