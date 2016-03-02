FROM nubomedia/apps-baseimage:v1

MAINTAINER Nubomedia

ADD . /

ENTRYPOINT mvn compile exec:java
