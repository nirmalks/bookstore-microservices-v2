# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

ARG MAVEN_VERSION=3.9.9
RUN apt-get update && apt-get install -y wget && \
    wget https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
    tar -xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
    mv apache-maven-${MAVEN_VERSION} /opt/maven && \
    rm apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
    apt-get clean && rm -rf /var/lib/apt/lists/*
ENV PATH="/opt/maven/bin:${PATH}"

COPY pom.xml .
COPY catalog-service/pom.xml catalog-service/
COPY checkout-service/pom.xml checkout-service/
COPY user-service/pom.xml user-service/
COPY shared-utils/pom.xml shared-utils/
COPY discovery-service/pom.xml discovery-service/
COPY api-gateway/pom.xml api-gateway/
COPY auth-server/pom.xml auth-server/
COPY shared-utils/src shared-utils/src
COPY config-server/pom.xml config-server/
COPY notification-service/pom.xml notification-service/
COPY audit-service/pom.xml audit-service/

RUN --mount=type=cache,target=/root/.m2 mvn clean install -DskipTests -pl shared-utils -am
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B
COPY . .

RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -pl config-server -am

FROM eclipse-temurin:25-jre-noble
WORKDIR /app
ENV ARTIFACT_NAME=config-server
COPY --from=build /app/${ARTIFACT_NAME}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]