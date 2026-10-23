// generated-by: generate_per_class_unit_tests.py
package org.upyog.chb.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalculationTypeCacheTest {
    @Mock
    private CommunityHallBookingConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ObjectMapper mapper;

    private CalculationTypeCache subject;

    @BeforeEach
    void setUp() {
        subject = new CalculationTypeCache(config, serviceRequestRepository, mapper);
    }


    @Test
    void getcalculationType_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
