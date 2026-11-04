package com.ingestpipeline.integration;

import com.ingestpipeline.IngestApp;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = IngestApp.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
                "spring.kafka.listener.auto-startup=false"
        })
@ActiveProfiles("test")
class IngestAppContextIT {

    @Test
    void contextLoads() {
    }
}
