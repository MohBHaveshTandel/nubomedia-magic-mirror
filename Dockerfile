FROM nubomedia/apps-baseimage:src

MAINTAINER Nubomedia

ADD . /

RUN mvn compile

ENTRYPOINT mvn exec:java
