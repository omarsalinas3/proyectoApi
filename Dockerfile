# Etapa 1: Compilación con Gradle y JDK 21
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copiar archivos de gradle y código fuente
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties ./
COPY src src

# Dar permisos de ejecución a gradlew y compilar JAR
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

# Etapa 2: Imagen final ligera para ejecución
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar el jar generado
COPY --from=build /app/build/libs/*.jar app.jar

# Exponer puerto 8080
EXPOSE 8080

# Iniciar aplicación
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
