package com.rector.app.utility;

import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TimeZone;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SensorData {

	private static final Logger logger = LogManager.getLogger(SensorData.class);
	
	private String location;
	private String sensor_name;
	private long epoch_sec;
	private float temperature;
	private float humidity;
	private String temperature_unit;
	
	private JsonObject jsonObject;

	
	/**
	 * 
	 */
	public SensorData() {
		
		// default data
		this.location = "nowhere";
		this.sensor_name = "none";
		this.epoch_sec = convertToEpochTime( "1970-01-01T00:00:00" );
		this.temperature = 0.0f;
		this.humidity = 0.0f;
		this.temperature_unit = "-";
		
	}

	/**
	 * 
	 * @param location
	 * @param sensor_name
	 * @param epoch_sec
	 * @param temperature
	 * @param humidity
	 */
	public SensorData( String location, String sensor_name, long epoch_sec, float temperature, float humidity, String temperature_unit ){
		
		this.location = location;
		this.sensor_name = sensor_name;
		this.epoch_sec = epoch_sec;
		this.temperature = temperature;
		this.humidity = humidity;
		this.temperature_unit = temperature_unit;
	}
	

	/**
	 * Topic : tele/dev/SENSOR, Message : {"Time":"2018-01-24T21:21:27","AM2301":{"Temperature":21.0,"Humidity":50.5},"TempUnit":"C"}
	 * 
	 * @param topic  tele/dev/SENSOR
	 * @param jsonData {"Time":"2018-01-24T21:21:27","AM2301":{"Temperature":21.0,"Humidity":50.5},"TempUnit":"C"}
	 * @param json
	 */
	public SensorData(String location, String jsonData){
		
		JsonReader reader = Json.createReader(new StringReader(jsonData));
         
		jsonObject = reader.readObject();
         
        reader.close();
        
        this.location = location; 
        
        Iterator<Entry<String, JsonValue>> e = jsonObject.entrySet().iterator();
        while (e.hasNext()) {
            Entry<String, JsonValue> pair = e.next();
            if( !(0 == pair.getKey().compareTo("Time") || 0 == pair.getKey().compareTo("TempUnit")) ) {      	
            	// get sensor name
        		this.sensor_name = pair.getKey();
            	logger.info( "Sensor node, Name : " +  this.sensor_name  );
            }
        }
        
		this.epoch_sec = convertToEpochTime( jsonObject.getString("Time") );

		JsonObject sensor = jsonObject.getJsonObject( this.sensor_name );
		
		this.temperature = (float) sensor.getJsonNumber("Temperature").doubleValue();
		this.humidity = (float) sensor.getJsonNumber("Humidity").doubleValue();
		
		this.temperature_unit = jsonObject.getString("TempUnit");
        
	}
	
	/**
	 * Convert time from mqtt message to epoch.
	 * "2018-01-29 12:56:15" to  1517226975
	 * @param time "2018-01-29T12:56:15" 
	 * @return long value in sec
	 * 
	 * 
	 */
	long convertToEpochTime( String time ){
		
		    try {
		    	String str = time.replace("T", " ");
		    	DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    	format.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
		    	Long millis = format.parse(str).getTime() / 1000;
		
				logger.debug("Converting " + time + " to : " + millis );
				return millis;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		return 0; // Human time (GMT): Thursday, January 1, 1970 12:00:00 AM
	}
	
	/**
	 * Convert time from epoch to mqtt message.
	 * 
	 * @param long value in sec
	 * @return String "2018-01-24T21:21:27"
	 * 
	 */
	String convertToMqttTime( long epoch ){
		
		Date date = new Date(epoch * 1000);
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		DateFormat tf = new SimpleDateFormat("HH:mm:ss");
		return df.format(date) + "T" +  tf.format(date);
		
//		return "1970-01-01T00:00:00"; // Epoch timestamp: 0
	}
	
	/**
	 * 
	 * @return
	 */
	public String toJson() {
		
		jsonObject = Json.createObjectBuilder()
				.add("Time", convertToMqttTime(this.epoch_sec))
				.add(this.sensor_name, Json.createObjectBuilder().add("Temperature", this.temperature)
													   			 .add("Humidity", this.humidity))
				.add("TempUnit", this.temperature_unit)
				.build();
		
		StringWriter stringWriter = new StringWriter();
        JsonWriter writer = Json.createWriter(stringWriter);
        writer.writeObject(this.jsonObject);
        writer.close();
//        System.out.println(stringWriter.getBuffer().toString());
        return stringWriter.getBuffer().toString();
        
		//return "{\"Time\":\"2018-01-24T21:21:27\",\"AM2301\":{\"Temperature\":21.0,\"Humidity\":50.5},\"TempUnit\":\"C\"}";
		
	}
	
	
	// getter and setter functions
	
	/**
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * @param location the location to set
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	/**
	 * @return the sensor_name
	 */
	public String getSensor_name() {
		return sensor_name;
	}

	/**
	 * @param sensor_name the sensor_name to set
	 */
	public void setSensor_name(String sensor_name) {
		this.sensor_name = sensor_name;
	}

	/**
	 * @return the epoch_sec
	 */
	public long getEpoch_sec() {
		return epoch_sec;
	}

	/**
	 * @param epoch_sec the epoch_sec to set
	 */
	public void setEpoch_sec(long epoch_sec) {
		this.epoch_sec = epoch_sec;
	}

	/**
	 * @return the temperature
	 */
	public float getTemperature() {
		return temperature;
	}

	/**
	 * @param temperature the temperature to set
	 */
	public void setTemperature(float temperature) {
		this.temperature = temperature;
	}

	/**
	 * @return the humidity
	 */
	public float getHumidity() {
		return humidity;
	}

	/**
	 * @param humidity the humidity to set
	 */
	public void setHumidity(float humidity) {
		this.humidity = humidity;
	}

	/**
	 * @return the temperature_unit
	 */
	public String getTemperature_unit() {
		return temperature_unit;
	}

	/**
	 * @param temperature_unit the temperature_unit to set
	 */
	public void setTemperature_unit(String temperature_unit) {
		this.temperature_unit = temperature_unit;
	}
	
}
