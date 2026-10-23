// generated-by: generate_per_class_unit_tests.py
package org.upyog.adv.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.upyog.adv.config.BookingConfiguration;
import org.upyog.adv.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MdmsUtilTest {
    @Mock
    private BookingConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ObjectMapper mapper;

    private MdmsUtil subject;

    @BeforeEach
    void setUp() {
        subject = new MdmsUtil(config, serviceRequestRepository, mapper);
    }


    @Test
    void mDMSCall_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
