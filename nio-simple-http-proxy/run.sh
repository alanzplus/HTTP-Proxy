#!/bin/bash
mvn package && clear && $JAVA_HOME/bin/java -jar target/nio-simple-http-proxy-1.0.0-SNAPSHOT-jar-with-dependencies.jar
