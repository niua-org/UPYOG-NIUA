package org.egov.wscalculation.service;

import org.egov.tracer.model.CustomException;
import org.egov.wscalculation.web.models.AdhocTaxReq;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class WSCalculationServiceImplTest {

    @InjectMocks
    private WSCalculationServiceImpl wsCalculationService;

    @Test
    void applyAdhocTax_whenInvalidBusinessService_throws() {
        AdhocTaxReq req = new AdhocTaxReq();
        req.setBusinessService("INVALID");
        req.setAdhocpenalty(BigDecimal.ZERO);
        req.setAdhocrebate(BigDecimal.ZERO);

        assertThatThrownBy(() -> wsCalculationService.applyAdhocTax(req))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Provide businessService is invalid");
    }
}
