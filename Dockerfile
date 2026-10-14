# ---- estágio 1: build ----
# Usa a imagem do JDK 21 e o Maven Wrapper do próprio projeto, então a
# versão do Maven é a mesma que você usa localmente.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copia primeiro só os arquivos de build. Assim, enquanto o pom.xml não
# mudar, o Docker reaproveita a camada de dependências já baixadas.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q -DskipTests dependency:resolve

# Agora copia o código-fonte e gera o jar.
COPY src ./src
RUN ./mvnw -B -q -DskipTests clean package

# ---- estágio 2: runtime ----
# Imagem final só com o JRE (menor) e o jar pronto.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Roda como usuário sem privilégios, não como root.
RUN useradd --system --create-home appuser
USER appuser

COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
