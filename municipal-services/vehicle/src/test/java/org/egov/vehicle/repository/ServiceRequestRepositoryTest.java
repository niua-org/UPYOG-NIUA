// generated-by: generate_per_class_unit_tests.py
package org.egov.vehicle.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ServiceRequestRepositoryTest {
    @Mock
    private ObjectMapper mapper;
    @Mock
    private RestTemplate restTemplate;

    private ServiceRequestRepository subject;

    @BeforeEach
    void setUp() {
        subject = new ServiceRequestRepository(mapper, restTemplate);
    }


    @Test
    void fetchResult_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
