// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.echallan.config.ChallanConfiguration;
import org.egov.echallan.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private ChallanConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ObjectMapper mapper;

    private UserService subject;

    @BeforeEach
    void setUp() {
        subject = new UserService(config, serviceRequestRepository, mapper);
    }


    @Test
    void getUser_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
