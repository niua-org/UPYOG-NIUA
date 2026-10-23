// generated-by: generate_per_class_unit_tests.py
package org.egov.vendor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.vendor.config.VendorConfiguration;
import org.egov.vendor.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BoundaryServiceTest {
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private VendorConfiguration config;

    private BoundaryService subject;

    @BeforeEach
    void setUp() {
        subject = new BoundaryService(serviceRequestRepository, mapper, config);
    }


    @Test
    void getAreaType_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
