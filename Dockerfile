FROM eclipse-temurin:17-jdk-alpine

WORKDIR /deutsche-bank

COPY build.gradle.kts ./settings.gradle.kts ./
COPY gradle.properties ./gradle.properties ./
COPY src/ ./src
COPY gradlew ./gradlew
COPY gradle/ ./gradle/

RUN ./gradlew buildFatJar && mv build/libs/CompanyTest-all.jar ./CompanyTest.jar
RUN rm -rf src build gradle gradlew build.gradle.kts settings.gradle.kts gradle.properties

EXPOSE 8080

CMD ["java", "-jar", "CompanyTest.jar"]
