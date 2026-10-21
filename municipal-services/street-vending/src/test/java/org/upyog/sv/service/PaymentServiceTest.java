// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.upyog.sv.config.StreetVendingConfiguration;
import org.upyog.sv.repository.ServiceRequestRepository;
import org.upyog.sv.repository.StreetVendingRepository;
import org.upyog.sv.service.DemandService;
import org.upyog.sv.service.StreetyVendingNotificationService;
import org.upyog.sv.service.UserService;
import org.upyog.sv.util.IdgenUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    private ObjectMapper mapper;
    @Mock
    private StreetVendingConfiguration configs;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private StreetVendingRepository streetVendingRepository;
    @Mock
    private IdgenUtil idgenUtil;
    @Mock
    private DemandService demandService;
    @Mock
    private UserService userService;
    @Mock
    private StreetyVendingNotificationService notificationService;

    private PaymentService subject;

    @BeforeEach
    void setUp() {
        subject = new PaymentService(mapper, configs, serviceRequestRepository, streetVendingRepository, idgenUtil, demandService, userService, notificationService);
    }


    @Test
    void process_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
