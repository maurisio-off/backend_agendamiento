# Etapa 1: compilación del proyecto con Maven y Java 21
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Da permisos de ejecución al wrapper de Maven y descarga las dependencias
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw clean package -DskipTests

# Etapa 2: imagen liviana de ejecución
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
