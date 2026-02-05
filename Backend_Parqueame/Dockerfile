# ───── STAGE 1: BUILD ─────
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# 1. Cache de dependencias de Gradle
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon --info

# 2. Copiar código fuente y compilar con más detalle
COPY src src
ENV GRADLE_OPTS="-Xmx1g -XX:MaxMetaspaceSize=256m"
RUN ./gradlew clean build --no-daemon --stacktrace --info

# ───── STAGE 2: RUNTIME ─────
FROM eclipse-temurin:17-jre
WORKDIR /app

# 3. Copiar el JAR generado
COPY --from=builder /app/build/libs/Backend_Parqueame-all.jar ./app.jar

# 4. Exponer puerto y arrancar la aplicación
EXPOSE ${PORT}
CMD ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]