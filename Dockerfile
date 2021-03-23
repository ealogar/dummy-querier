FROM maven:3.6.1-jdk-11 as mvn
ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "/root/.m2"
ENV MAVEN_OPTS "-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true"

ARG AWS_SECRET_ACCESS_KEY
ARG AWS_ACCESS_KEY_ID

WORKDIR /opt/app

COPY pom.xml ./pom.xml

RUN mvn dependency:resolve-plugins dependency:resolve clean package

COPY src/ ./src/

RUN mvn clean package

FROM openjdk:11.0.6-slim

WORKDIR /opt/app

COPY --from=mvn  /opt/app/target/dummy-querier-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/app/dummy-querier-1.0.jar

COPY docker-entrypoint.sh /opt/app/

ENTRYPOINT ["/opt/app/docker-entrypoint.sh"]
