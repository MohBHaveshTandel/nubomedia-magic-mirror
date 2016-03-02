FROM nubomedia/apps-baseimage:v1

MAINTAINER Nubomedia

ADD . /

RUN mvn compile

ENTRYPOINT mvn exec:java
