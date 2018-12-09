package com.rector.app.ws.resolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import com.rector.app.ws.model.Sensor;
import com.rector.app.ws.repository.SensorRepository;
import com.rector.app.ws.service.InputSensor;
import com.rector.app.ws.service.SensorService;

import lombok.extern.slf4j.Slf4j;


// https://g00glen00b.be/graphql-spring-boot/
@Slf4j
@Component
public class Queries implements GraphQLQueryResolver {

	@Autowired
	SensorRepository sensorRepository;

	@Autowired
	SensorService sensorSevice;
	
	public Iterable<Sensor> allsensors() {
		log.info("sensors()");
        return sensorRepository.findByOrderByEpochAsc();
    }
	
	public Iterable<Sensor> sensors(final InputSensor sensor) {	
        return sensorSevice.find( sensor );
    }
}
