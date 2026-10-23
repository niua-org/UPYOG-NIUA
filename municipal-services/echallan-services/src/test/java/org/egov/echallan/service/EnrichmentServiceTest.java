// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.echallan.config.ChallanConfiguration;
import org.egov.echallan.repository.ChallanRepository;
import org.egov.echallan.repository.IdGenRepository;
import org.egov.echallan.repository.ServiceRequestRepository;
import org.egov.echallan.service.UserService;
import org.egov.echallan.util.CommonUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceTest {
    @Mock
    private IdGenRepository idGenRepository;
    @Mock
    private ChallanConfiguration config;
    @Mock
    private CommonUtils commonUtils;
    @Mock
    private UserService userService;
    @Mock
    private ChallanRepository challanRepository;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private EnrichmentService subject;

    @BeforeEach
    void setUp() {
        subject = new EnrichmentService(idGenRepository, config, commonUtils, userService, challanRepository, serviceRequestRepository);
    }


    @Test
    void enrichCreateRequest_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
