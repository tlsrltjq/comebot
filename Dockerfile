FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle/
RUN ./gradlew dependencies --no-daemon -q
COPY src src/
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/comebot-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
