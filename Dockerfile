FROM openjdk:17-slim-bullseye

RUN apt-get update && \
    apt-get install -y maven default-mysql-client python-is-python2

WORKDIR /app
# Cache plugins by calling help once before use
# https://stackoverflow.com/a/39336178
RUN mvn exec:help
COPY pom.xml pom.xml
RUN mvn dependency:go-offline
ADD . /app
RUN mvn compile package
