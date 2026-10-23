// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.util;

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
class CommonUtilsTest {
    @Mock
    private ObjectMapper mapper;
    @Mock
    private ChallanConfiguration configs;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private CommonUtils subject;

    @BeforeEach
    void setUp() {
        subject = new CommonUtils(mapper, configs, serviceRequestRepository);
    }


    @Test
    void getAuditDetails_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
