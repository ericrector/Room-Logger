FROM maven:3.6.0-jdk-8-alpine

# copy / checkout code
COPY ./testapp/target/test-app-0.0.1-SNAPSHOT.jar /tmp/test-app-0.0.1-SNAPSHOT.jar

#RUN set -x; \
#	java -cp /tmp/test-app-0.0.1-SNAPSHOT.jar com.rector.app.Main
	
# compile in container
#RUN set -x; \
#	cd /tmp/SensorDataImporter
#	javac 

# start importer service

## test
##COPY SensorValueImporter.jar /usr/local/bin/SensorValueImporter.jar
##CMD ["java","-jar","/usr/local/bin/SensorValueImporter.jar"]

## as service
#COPY SensorValueImporter.jar /usr/local/bin/SensorValueImporter.jar
#COPY SensorDataImporter /etc/init.d/SensorDataImporter
##update-rc.d mytestserv defaults
#CMD ["update-rc.d","SensorDataImporter","defaults"]
#CMD ["service","SensorDataImporter","start"]

CMD ["/bin/sh", "-c", "java -cp /tmp/test-app-0.0.1-SNAPSHOT.jar com.rector.app.Main"]

#docker build -t java/console:0.1 .

#docker run -it --rm java/console:0.1

#docker run -it --rm "DB_NAME=tdb" -e "DB_HOST=myHost" -e "DB_USER=me" -e "DB_PASS=shhhh" -e "DB_PORT=0000"  maven/console:0.3
#or 
#docker build -t -env--file=variables.env  maven/console:0.3 .