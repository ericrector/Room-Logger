package com.rector.app.ws.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rector.app.ws.model.Sensor;
import com.rector.app.ws.repository.SensorRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class SensorServiceImpl implements SensorService {

	@Autowired
	SensorRepository repository;
	
	@Override
	public List<Sensor> find(InputSensor filter) {
		
		// TODO Auto-generated method stub
		log.info( "room: " + filter.getRoom() );
		
		return repository.findByRoomOrderByEpochAsc(filter.getRoom());
	}

}
