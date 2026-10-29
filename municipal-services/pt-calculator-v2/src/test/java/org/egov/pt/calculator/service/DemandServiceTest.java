package org.egov.pt.calculator.service;

import org.egov.pt.calculator.web.models.CalculationReq;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class DemandServiceTest {

    @InjectMocks
    private DemandService demandService;

    @Test
    void generateDemands_whenCriteriaNull_throws() {
        CalculationReq req = new CalculationReq();
        req.setCalculationCriteria(null);

        assertThatThrownBy(() -> demandService.generateDemands(req))
                .isInstanceOf(NullPointerException.class);
    }
}
