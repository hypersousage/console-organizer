FROM gradle:6.7-jre15 AS build
WORKDIR /app
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
COPY src ./src
RUN ./gradlew build

FROM openjdk:15.0.2-jdk-slim
COPY --from=build /app/build/libs/ConsoleOrganizer-2.0-SNAPSHOT.jar ConsoleOrganizer-2.0-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "ConsoleOrganizer-2.0-SNAPSHOT.jar"]