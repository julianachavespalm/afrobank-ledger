# Estágio 1: Build da Aplicação
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Estágio 2: Imagem de Runtime Leve e Segura
FROM eclipse-temurin:21-jre-noble
WORKDIR /app

# Usuário não-root por compliance e segurança bancária
RUN addgroup --system afrobank && adduser --system --ingroup afrobank afrobank
USER afrobank:afrobank

COPY --from=builder /app/target/*.jar app.jar

ENV PORT=8080
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
