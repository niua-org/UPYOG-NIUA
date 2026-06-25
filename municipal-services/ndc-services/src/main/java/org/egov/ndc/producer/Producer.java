package org.egov.ndc.producer;

import java.util.UUID;

import org.egov.tracer.kafka.CustomKafkaTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Producer {

	@Autowired
	private CustomKafkaTemplate<String, Object> kafkaTemplate;

	public void push(String topic, Object value) {
		String key = UUID.randomUUID().toString();
		kafkaTemplate.send(topic, key, value);
	}

	public void push(String topic, String key, Object value) {
		kafkaTemplate.send(topic, key, value);
	}
}
