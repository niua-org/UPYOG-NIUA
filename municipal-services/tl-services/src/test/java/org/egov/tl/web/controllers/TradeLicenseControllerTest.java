// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.web.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.egov.tl.service.TradeLicenseService;
import org.egov.tl.service.notification.PaymentNotificationService;
import org.egov.tl.service.notification.TLNotificationService;
import org.egov.tl.util.ResponseInfoFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TradeLicenseControllerTest {
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private TradeLicenseService tradeLicenseService;
    @Mock
    private ResponseInfoFactory responseInfoFactory;
    @Mock
    private PaymentNotificationService paymentNotificationService;
    @Mock
    private TLNotificationService tlNotificationService;

    private TradeLicenseController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new TradeLicenseController(objectMapper, request, tradeLicenseService, responseInfoFactory, paymentNotificationService, tlNotificationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void mockMvc_whenStandaloneSetup_isNotNull() {
        assertThat(mockMvc).isNotNull();
    }
}
