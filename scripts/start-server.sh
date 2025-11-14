#!/bin/bash

# Start the Java EDS server with IPv4 only
java -Djava.net.preferIPv4Stack=true -jar target/envoyjavaedsserver-1.0-SNAPSHOT-jar-with-dependencies.jar