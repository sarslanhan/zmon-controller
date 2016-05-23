#!/bin/bash

if [ ! -f zmon-controller-app/target/zmon-controller-1.0.1-SNAPSHOT.jar ]; then
	./mvnw clean install
fi

echo 'Make sure the provided Vagrant-Box is up and all services are running.'

export ZMON_AUTHORITIES_SIMPLE_ADMINS=*  # everybody is admin!
export REDIS_PORT=38086                  # use Redis in Vagrant box
export POSTGRES_URL=jdbc:postgresql://localhost:38088/local_zmon_db
export ZMON_SCHEDULER_URL=http://localhost:38085
java -jar zmon-controller-app/target/zmon-controller-1.0.1-SNAPSHOT.jar

