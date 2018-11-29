package com.rector.app;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.rector.app.utility.SensorData;

public class SensorDataImporter {

	private static final Logger logger = LogManager.getLogger(SensorDataImporter.class);

	// DB
	private static final String DB_DRIVER = "org.gjt.mm.mysql.Driver";
	private String DB_HOST;
	private int    DB_PORT;
	private String DB_USER;
	private String DB_PASS;
	private String DB_NAME;
	
	// MQTT
	private String MQTT_HOST;
	private int    MQTT_PORT;
	private String MQTT_USER;
	private String MQTT_PASS;
	private String MQTT_TOPIC;
	
	// database
	Connection db_connection = null;
	
	Connection getConnection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {

		if( null == db_connection ) {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			db_connection = DriverManager
			.getConnection("jdbc:mysql://" + DB_HOST + "/" + DB_NAME + "?user=" + DB_USER + "&password=" + DB_PASS);
		}
		
		return db_connection;
	}
	
	private Object END_SIGNAL = new Object();
	
	// MQTT
	CallbackConnection mqtt_connection = null;
	
	private void readEnvValues() throws Exception {
		
		Map<String, String> env = System.getenv();
		
		DB_HOST = env.get("DB_HOST");
		String port = env.get("DB_PORT"); 
		DB_PORT = Integer.parseInt(port);
		DB_USER = env.get("DB_USER");
		DB_PASS = env.get("DB_PASS");
		DB_NAME = env.get("DB_NAME");
		MQTT_HOST = env.get("MQTT_HOST");
		port = env.get("MQTT_PORT");
		MQTT_PORT = Integer.parseInt(port);
		MQTT_USER = env.get("MQTT_USER");
		MQTT_PASS = env.get("MQTT_PASS");
		MQTT_TOPIC = env.get("MQTT_TOPIC");

		// Check to see if we are missing anything
		// if so fire an exception
		//throw new Exception("Invalid variable found. Add it to enviorment");
	}
	
	public SensorDataImporter() throws Exception {

		readEnvValues();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {

				logger.info("Shuting down ...");

				END_SIGNAL.notifyAll();
				
				logger.debug("Waiting ...");
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					logger.error( e.getMessage() );
				}
				
				logger.info("Finished ...");
			}
		});
		
		// try db connection
		try {
			logger.info("Connecting to database...");
			getConnection();
			logger.info("Database connection ok...");
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
			logger.error( e.getMessage() );
		}
		
		MQTT mqtt = new MQTT();
		
		try {
			mqtt.setHost(MQTT_HOST, MQTT_PORT);
		} catch (URISyntaxException e) {
			logger.error( e.getMessage() );
			return;
		}

		logger.info("Connecting to broker...");
		
		mqtt_connection = mqtt.callbackConnection();

		mqtt_connection.listener(new Listener() {

			@Override
			public void onFailure(Throwable arg0) {
				logger.error("Connectin to broker failed : " + MQTT_HOST + ":" + MQTT_PORT );
				END_SIGNAL.notifyAll();
			}

			@Override
			public void onDisconnected() {
				logger.debug("Disconnected! (MQTT)");
			}

			@Override
			public void onConnected() {
				logger.debug("Connected! (MQTT)");
			}

			@Override
			public void onPublish(UTF8Buffer topic, Buffer payload, Runnable ack) {
				// You can now process a received message from a topic.
				// Once process execute the ack runnable.
				logger.debug("Received message : " + payload);			
				
				ack.run();
				
				try {
					
					String messagePayload = new String(payload.toByteArray(), "UTF-8");		
					String[] parts = topic.toString().split("/");

					// TODO Add to a worker queue and do this work there!
					importMessage( new SensorData( parts[1], messagePayload ) );
					
				} catch (MySQLIntegrityConstraintViolationException e) {
					logger.error( "Duplaicate : " + e.getMessage() );
				} catch (UnsupportedEncodingException e) {
					logger.error( e.getMessage() );
				} catch (Exception e) {
					logger.error( e.getMessage() );
				}
				
				
			}

		});

		mqtt_connection.connect( new Callback<Void>() {
			
			@Override
			public void onSuccess(Void arg0) {
				logger.debug("Broker connection ok!");
				
				// Subscribe to a topic
		        Topic[] topics = {new Topic(MQTT_TOPIC, QoS.AT_LEAST_ONCE)};
		        
		        mqtt_connection.subscribe(topics, new Callback<byte[]>() {
		            public void onSuccess(byte[] qoses) {
		            	logger.debug("Subscribe sucessfull!");
		            }
		            public void onFailure(Throwable value) {
		            	logger.error("Subscribe failed!");
		            	END_SIGNAL.notifyAll();   
		            }
		        });
				
			}
			
			@Override
			public void onFailure(Throwable arg0) {
				logger.error("Broker connection failed!");
				END_SIGNAL.notifyAll();
			}
		});

		
		synchronized (END_SIGNAL) {
			try {
				END_SIGNAL.wait();
			} catch (InterruptedException e) {
				logger.debug("Exiting app!");
			}
		}

		
		mqtt_connection.disconnect( new Callback<Void>() {
			
			@Override
			public void onSuccess(Void arg0) {
				logger.debug("Disconnected!");
				
			}
			
			@Override
			public void onFailure(Throwable arg0) {
				logger.error("Could not disconnected!");
				
			}
		});
		
		mqtt_connection = null;
		
		mqtt = null;
		
		// close db!
		try {
			getConnection().close();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
			logger.error( e.getMessage() );
		}
		
		logger.info("Finished!");
	}

	protected void importMessage(SensorData sensorData) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {

		logger.debug( "Location : " + sensorData.getLocation() + ", Data : " + sensorData.toJson()  );

		String query = "INSERT INTO Sensor(room, name, epoch, temperature, humidity) VALUES "
		+ "(?,?,?,?,?)";
		
//		String query = "INSERT INTO \"Sensor\"(\"room\", \"name\", \"epoch\", \"temperature\", \"humidity\") VALUES "
//				+ "(?,?,?,?,?,?)";
		
		// create the mysql insert preparedstatement
	      PreparedStatement preparedStmt = getConnection().prepareStatement(query);
	      preparedStmt.setString (1, sensorData.getLocation());
	      preparedStmt.setString (2, sensorData.getSensor_name());
	      preparedStmt.setLong(3, sensorData.getEpoch_sec());
	      preparedStmt.setFloat(4, sensorData.getTemperature());
	      preparedStmt.setFloat(5, sensorData.getHumidity());

	      // execute the preparedstatement
	      preparedStmt.execute();
	      
	}

	public static void main(String[] args) { 
		logger.info("Entering SensorDataImporter.");
		try {
			new SensorDataImporter();
		} catch (Exception e) {
			logger.error( e.getMessage() );
		}
		logger.info("Exiting SensorDataImporter.");
	}

}
