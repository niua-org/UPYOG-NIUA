package org.egov.echallancalculation.service;

import org.egov.echallancalculation.web.models.calculation.CalculationReq;
import org.egov.echallancalculation.web.models.calculation.CalulationCriteria;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.echallancalculation.util.CalculationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.echallancalculation.config.ChallanConfiguration;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

    @Mock private DemandService demandService;
    @Mock private CalculationUtils utils;
    @Mock private ObjectMapper objectMapper;
    @Mock private ChallanConfiguration config;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private CalculationService calculationService;

    @Test
    void updateCalculation_whenDemandIdMissing_throws() {
        CalulationCriteria criteria = new CalulationCriteria();
        criteria.setDemandId(null);
        CalculationReq req = new CalculationReq();
        req.setCalulationCriteria(Collections.singletonList(criteria));

        assertThatThrownBy(() -> calculationService.updateCalculation(req))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("demandId is required");
    }
}
