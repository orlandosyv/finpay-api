FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp package -DskipTests

FROM eclipse-temurin:25-jre-noble

RUN groupadd --system finpay \
    && useradd --system --gid finpay --home-dir /app finpay

WORKDIR /app

COPY --from=build --chown=finpay:finpay /workspace/target/finpay-api-*.jar app.jar

USER finpay

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
