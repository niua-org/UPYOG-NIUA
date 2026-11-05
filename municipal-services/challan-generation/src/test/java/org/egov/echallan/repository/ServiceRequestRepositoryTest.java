// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.repository;

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
    private RestTemplate restTemplate;
    @Mock
    private ObjectMapper mapper;

    private ServiceRequestRepository subject;

    @BeforeEach
    void setUp() {
        subject = new ServiceRequestRepository(restTemplate, mapper);
    }


    @Test
    void fetchResult_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
