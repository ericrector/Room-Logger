package com.rector.app.ws.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rector.app.ws.model.Sensor;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, String> {
	
	List<Sensor> findByOrderByEpochAsc();

	List<Sensor> findByRoomOrderByEpochAsc(String room);
	
}
