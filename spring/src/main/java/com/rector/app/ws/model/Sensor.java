package com.rector.app.ws.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "Sensor")
public class Sensor {

	@Id
	private String id;
	
	private String name;
	private String room;
	private int epoch;
	private float temperature;
	private float humidity;
	
	
}