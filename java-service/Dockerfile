FROM openjdk:8

COPY ./SensorDataImporter/target/SensorDataImporter-0.0.1-SNAPSHOT-jar-with-dependencies.jar /tmp/SensorDataImporter.jar
COPY ./SensorDataImporter/resources/log4j2.xml /tmp/log4j2.xml

CMD java -Dlog4j.configurationFile=/tmp/log4j2.xml -jar /tmp/SensorDataImporter.jar
