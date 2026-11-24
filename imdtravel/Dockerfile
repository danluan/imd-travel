# Estágio de build: compila o jar (os testes NÃO rodam aqui; ver `mvnw test`).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /w
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Camada de dependências: só é refeita se o pom.xml mudar.
RUN sh mvnw -B -q dependency:go-offline
COPY src src
RUN sh mvnw -B -q package -DskipTests \
 && cp target/*.jar /w/app.jar

# Estágio de runtime
FROM eclipse-temurin:21-jre
# curl: usado pelo healthcheck do docker-compose.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*
# Usuário não-root; /data pertence a ele para o volume nomeado herdar a posse.
RUN groupadd --system app && useradd --system --gid app --home-dir /app app \
 && mkdir -p /app /data && chown app:app /data
COPY --from=build /w/app.jar /app/app.jar
ENV SALES_DIR=/data PORT=8080
EXPOSE 8080
USER app
ENTRYPOINT ["java","-jar","/app/app.jar"]
