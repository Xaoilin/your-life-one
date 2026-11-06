FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY services/common-service/pom.xml services/common-service/pom.xml
COPY services/prayer-service/pom.xml services/prayer-service/pom.xml
COPY services/calendar-service/pom.xml services/calendar-service/pom.xml
COPY services/profile-service/pom.xml services/profile-service/pom.xml
COPY services/life-service/pom.xml services/life-service/pom.xml
COPY services/knowledge-service/pom.xml services/knowledge-service/pom.xml
COPY services/planner-service/pom.xml services/planner-service/pom.xml
COPY services/finance-service/pom.xml services/finance-service/pom.xml
COPY services/live-service/pom.xml services/live-service/pom.xml
RUN ./mvnw -B -ntp -pl services/finance-service -am dependency:go-offline

COPY services/common-service/src services/common-service/src
COPY services/finance-service/src services/finance-service/src
RUN ./mvnw -B -ntp -pl services/finance-service -am package -DskipTests -Dcheckstyle.skip

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build --chown=app:app /workspace/services/finance-service/target/finance-service.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
