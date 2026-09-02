# ============================================================
#  Etapa 1: compilacion con Maven
#  Se copia primero el pom.xml solo para que la descarga de
#  dependencias quede en una capa cacheada: mientras el pom no
#  cambie, Docker reutiliza esa capa y el build tarda segundos.
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# Los tests NO corren aca: corren en el job de CI (Etapa 4).
# Meterlos en el build de imagen haria que cada deploy tarde el doble
# y que un test flaky rompa el despliegue en vez del pipeline.
RUN mvn -B clean package -DskipTests

# ============================================================
#  Etapa 2: imagen final (solo el JRE, sin Maven ni el codigo)
# ============================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuario sin privilegios. Si el proceso se ve comprometido, no corre como root.
RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/archivos \
    && chown -R app:app /app

COPY --from=build /build/target/*.jar app.jar
RUN chown app:app /app/app.jar

USER app
EXPOSE 8080

# JAVA_OPTS permite ajustar memoria sin rebuildear la imagen.
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
