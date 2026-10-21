// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.calculator.kafka.broker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.tracer.kafka.CustomKafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NDCCalculatorProducerTest {
    @Mock
    private CustomKafkaTemplate<String, Object> kafkaTemplate;

    private NDCCalculatorProducer subject;

    @BeforeEach
    void setUp() {
        subject = new NDCCalculatorProducer(kafkaTemplate);
    }


    @Test
    void push_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
