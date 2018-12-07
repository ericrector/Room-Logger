package com.rector.app;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import com.rector.app.utility.SensorData;

public class SensorDataImporter {

	private static final Logger logger = LogManager.getLogger(SensorDataImporter.class);

	class Config {
		// DB
		String dbHost;
		int dbPort = 3306;
		String dbUser;
		String dbPass;
		String dbName;

		// MQTT
		String mqttHost;
		int mqttPort = 1883;
		String mqttUser = "";
		String mqttPass = "";
		String mqttTopic;

		public void validate() throws Exception {

			logger.info("db host: " + ((null != dbHost) ? dbHost : "INVALID") + ", port: " + dbPort + ", name: "
					+ ((null != dbName) ? dbName : "INVALID") + ", user: " + ((null != dbPass) ? dbPass : "INVALID")
					+ ", pass: " + ((null != dbPass) ? "****" : "INVALID"));

			logger.info("mqtt host: " + ((null != mqttHost) ? mqttHost : "INVALID") + ", port: " + mqttPort + ", user: "
					+ ((null != mqttUser) ? mqttUser : "N/A") + ", pass: " + ((null != mqttPass) ? "****" : "N/A")
					+ ", topic: " + ((null != mqttTopic) ? mqttTopic : "INVALID"));

			// check all members for null
			if (null == dbHost || null == dbHost || null == dbHost)
				throw new Exception("invalid configuration");

			if (null == mqttHost || null == mqttTopic)
				throw new Exception("invalid configuration");

		}
	}

	// database
	Connection db_connection = null;

	Connection getConnection(Config c)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {

		if (null == db_connection) {
			String connStr = "jdbc:mysql://" + c.dbHost + "/" + c.dbName + "?user=" + c.dbUser + "&password="
					+ c.dbPass;
			logger.info(connStr);
			db_connection = DriverManager.getConnection(connStr);
		}

		return db_connection;
	}

	private Object END_SIGNAL = new Object();

	// MQTT
	CallbackConnection mqtt_connection = null;

	public SensorDataImporter(String[] args) throws Exception {

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {

				logger.info("Shuting down ...");

				END_SIGNAL.notifyAll();

				logger.debug("Waiting ...");
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				}

				logger.info("Finished ...");
			}
		});
		
		Class.forName("com.mysql.cj.jdbc.Driver").newInstance();

		Config c = new Config();
		// read this in as default
		readEnvValues(c);

		String configFile = null;

		if (0 < args.length) {
			configFile = new String(args[args.length - 1]); // last one is config-file

			// over-ride values with config
			readconfigFile(c, configFile);

			// over-ride those with command line args.
			// readconfigCLI(c, args);

		}

		c.validate();

		logger.info("Connecting to database...");
		// try db connection
		boolean connected = false;
		for (int i = 0; i < 3; i++) {
			try {
				if (null != getConnection(c)) {
					connected = true;
					break;
				}
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
				logger.error(e.getMessage());
			}

			logger.info("Trying again in 10 sec...");

			try {
				Thread.sleep(10 * 1000);
			} catch (InterruptedException e) {
			}

		}

		if (!connected)
			return;

		logger.info("Database connection ok...");

		// see if table exists and create it if needed.
		prepareDatabase(getConnection(c));

		MQTT mqtt = new MQTT();

		try {
			mqtt.setHost(c.mqttHost, c.mqttPort);
		} catch (URISyntaxException e) {
			logger.error(e.getMessage());
			return;
		}

		logger.info("Connecting to broker...");

		mqtt_connection = mqtt.callbackConnection();

		mqtt_connection.listener(new Listener() {

			@Override
			public void onFailure(Throwable arg0) {
				logger.error("Connectin to broker failed : " + c.mqttHost + ":" + c.mqttPort);
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
					importMessage(getConnection(c), new SensorData(parts[1], messagePayload));

				} catch (UnsupportedEncodingException e) {
					logger.error(e.getMessage());
				} catch (Exception e) {
					logger.error(e.getMessage());
				}

			}

		});

		mqtt_connection.connect(new Callback<Void>() {

			@Override
			public void onSuccess(Void arg0) {
				logger.debug("Broker connection ok!");

				// Subscribe to a topic
				Topic[] topics = { new Topic(c.mqttTopic, QoS.AT_LEAST_ONCE) };

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

		mqtt_connection.disconnect(new Callback<Void>() {

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
			getConnection(c).close();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
			logger.error(e.getMessage());
		}

		logger.info("Finished!");
	}

	private void prepareDatabase(Connection conn) throws SQLException {

		logger.info("Checking for table...");
		String query = "SELECT * FROM information_schema.tables WHERE table_schema = 'myDb' AND table_name = 'Sensor' LIMIT 1;";

		// create the java statement
		Statement st = conn.createStatement();

		if( 0 == st.getMaxRows() ) {
			logger.info("Creating table.");
			// create the table
			query = "CREATE TABLE `myDb`.`Sensor` ( " + "`id` INT NOT NULL AUTO_INCREMENT, "
					+ "`room` VARCHAR(255) NOT NULL , " + "`name` VARCHAR(255) NOT NULL , " + "`epoch` INT NOT NULL , "
					+ "`temperature` FLOAT NOT NULL , " + "`humidity` FLOAT NOT NULL , INDEX (`id`)) " + "ENGINE = InnoDB;";
	
			PreparedStatement preparedStmt = conn.prepareStatement(query);
			preparedStmt.execute();
		} else
			logger.info("Table exisits.");

	}

	private void readEnvValues(Config c) throws Exception {

		logger.info("Reading system variables...");

		String port;

		Map<String, String> env = System.getenv();

		if (env.containsKey("DB_HOST"))
			c.dbHost = env.get("DB_HOST");

		if (env.containsKey("DB_PORT")) {
			port = env.get("DB_PORT");
			if (null != port)
				c.dbPort = Integer.parseInt(port);
		}

		if (env.containsKey("DB_USER"))
			c.dbUser = env.get("DB_USER");

		if (env.containsKey("DB_PASS"))
			c.dbPass = env.get("DB_PASS");

		if (env.containsKey("DB_NAME"))
			c.dbName = env.get("DB_NAME");

		if (env.containsKey("MQTT_HOST"))
			c.mqttHost = env.get("MQTT_HOST");

		if (env.containsKey("MQTT_PORT")) {
			port = env.get("MQTT_PORT");
			if (null != port)
				c.mqttPort = Integer.parseInt(port);
		}

		if (env.containsKey("MQTT_USER"))
			c.mqttUser = env.get("MQTT_USER");

		if (env.containsKey("MQTT_PASS"))
			c.mqttPass = env.get("MQTT_PASS");

		if (env.containsKey("MQTT_TOPIC"))
			c.mqttTopic = env.get("MQTT_TOPIC");

	}

	private void readconfigFile(Config c, String configFile) throws Exception {

		logger.info("Reading from configuration file ...");

		logger.warn("readconfigFile() not implemented");

	}

	protected void importMessage(Connection conn, SensorData sensorData)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {

		logger.debug("Location : " + sensorData.getLocation() + ", Data : " + sensorData.toJson());

		String query = "INSERT INTO Sensor(room, name, epoch, temperature, humidity) VALUES " + "(?,?,?,?,?)";

//		String query = "INSERT INTO \"Sensor\"(\"room\", \"name\", \"epoch\", \"temperature\", \"humidity\") VALUES "
//				+ "(?,?,?,?,?,?)";

		// create the mysql insert preparedstatement
		PreparedStatement preparedStmt = conn.prepareStatement(query);
		preparedStmt.setString(1, sensorData.getLocation());
		preparedStmt.setString(2, sensorData.getSensor_name());
		preparedStmt.setLong(3, sensorData.getEpoch_sec());
		preparedStmt.setFloat(4, sensorData.getTemperature());
		preparedStmt.setFloat(5, sensorData.getHumidity());

		// execute the preparedstatement
		preparedStmt.execute();

	}

	public static void main(String[] args) {
		logger.info("Entering SensorDataImporter.");
		try {
			new SensorDataImporter(args);
		} catch (Exception e) {
			logger.error("Error: " + e.getMessage());
		}
		logger.info("Exiting SensorDataImporter.");
	}

}
