FROM eclipse-temurin:22-jdk AS build

WORKDIR /app

COPY target/service-api-gen-shaded.jar ./service-api-gen.jar

CMD ["java", "-jar", "service-api-gen.jar"]
