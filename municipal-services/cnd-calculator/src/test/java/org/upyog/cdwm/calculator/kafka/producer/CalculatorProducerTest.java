// generated-by: generate_per_class_unit_tests.py
package org.upyog.cdwm.calculator.kafka.producer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.tracer.kafka.CustomKafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculatorProducerTest {
    @Mock
    private CustomKafkaTemplate<String, Object> kafkaTemplate;

    private CalculatorProducer subject;

    @BeforeEach
    void setUp() {
        subject = new CalculatorProducer(kafkaTemplate);
    }


    @Test
    void push_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
