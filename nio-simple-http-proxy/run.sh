#!/bin/bash
JVM_OPTS="-server -Xss228K -Xms512M -Xmx512M -XX:NewRatio=4 -XX:SurvivorRatio=8 -XX:TargetSurvivorRatio=90 -XX:+UseParNewGC -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=60 -XX:+CMSScavengeBeforeRemark -XX:+CMSParallelRemarkEnabled -XX:+CMSClassUnloadingEnabled -XX:+UseCompressedOops -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintClassHistogram -XX:+CMSConcMarkSweepGC"
mvn package && clear && $JAVA_HOME/bin/java ${JVM_OPTS} -jar target/nio-simple-http-proxy-1.0.0-SNAPSHOT-jar-with-dependencies.jar
