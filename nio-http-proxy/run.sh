#!/bin/bash
PROXY_OPTS="-Dworker=16 -Dport=9999 -DbufferSize=20 -DminNumBuffers=200 -DmaxNumBuffers=400 -DenableMonitor=true -DuseDirectBuffer=true -DmonitorUpdateInterval=15"
JVM_OPTS="-server -Xss228K -Xms512M -Xmx512M -XX:NewRatio=1 -XX:SurvivorRatio=8 -XX:TargetSurvivorRatio=90 -XX:+UseParNewGC -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=60 -XX:+CMSScavengeBeforeRemark -XX:+CMSParallelRemarkEnabled -XX:+CMSClassUnloadingEnabled -XX:+UseCompressedOops -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintClassHistogram -XX:+UseConcMarkSweepGC -verbose:gc -Xloggc:gc.log"
mvn package && clear && $JAVA_HOME/bin/java ${JVM_OPTS} ${PROXY_OPTS} -jar target/*-jar-with-dependencies.jar

