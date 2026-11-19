package org.egov.echallan.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.echallan.config.ChallanConfiguration;
import org.egov.echallan.model.ChallanRequest;
import org.egov.echallan.repository.ServiceRequestRepository;
import org.egov.tracer.model.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

    @Mock private ServiceRequestRepository serviceRequestRepository;
    @Mock private ObjectMapper mapper;
    @Mock private ChallanConfiguration config;

    @InjectMocks
    private CalculationService calculationService;

    @Test
    void addCalculation_whenChallanNull_throws() {
        ChallanRequest request = ChallanRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .build();

        assertThatThrownBy(() -> calculationService.addCalculation(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("empty or null");
    }
}
