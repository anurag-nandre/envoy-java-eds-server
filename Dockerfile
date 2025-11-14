

## Use an official Maven image to build the application
FROM registry.twilio.com/library/java/build-maven-jdk-17-corretto as builder

WORKDIR /app
COPY . /app
#RUN #mvn dependency:go-offline
RUN mvn clean package -DskipTests
#
FROM registry.twilio.com/library/java/jdk-17-corretto-debian-slim

#RUN #useradd twilio
COPY --from=builder --chown=twilio:twilio app/target/envoyjava  /app/bin/anurag-test-0.0.1-SNAPSHOT.jar

WORKDIR /app

#COPY --from=builder app/target/anurag-test-0.0.1-SNAPSHOT.jar ./anurag-test-0.0.1-SNAPSHOT.jar




COPY --chown=twilio:twilio docker-entrypoint.sh /app/bin/
RUN chmod +x /app/bin/docker-entrypoint.sh
USER twilio
ENTRYPOINT ["/app/bin/docker-entrypoint.sh"]

