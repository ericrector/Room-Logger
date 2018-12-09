package com.rector.app.ws.service;

import java.util.List;

import com.rector.app.ws.model.Sensor;

public interface SensorService {
	List<Sensor> find(InputSensor filter);
}
