FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
COPY test ./test
RUN mvn -B clean verify dependency:copy-dependencies \
    -DincludeScope=runtime \
    -DoutputDirectory=target/dependency

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/classes ./target/classes
COPY --from=build /app/target/dependency ./target/dependency
ENV PORT=8080
EXPOSE 8080
CMD ["sh", "-c", "exec java -cp 'target/classes:target/dependency/*' Main"]
