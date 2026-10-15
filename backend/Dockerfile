# ---------- Etapa 1: compilar el JAR ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Descargamos dependencias primero para aprovechar la caché de capas de Docker.
# El `|| true` evita que un fallo de este paso (opcional) rompa el build.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B -DskipTests clean package

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Ejecutamos como usuario sin privilegios.
RUN useradd -r -u 1001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

# Render inyecta PORT; application.properties usa ${PORT:8080}.
EXPOSE 8080

# JAVA_OPTS permite ajustar la memoria (p. ej. -Xmx384m en el plan gratuito).
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
