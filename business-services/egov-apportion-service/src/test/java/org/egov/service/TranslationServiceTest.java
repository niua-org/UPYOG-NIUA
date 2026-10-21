package org.egov.service;

import org.egov.tracer.model.CustomException;
import org.egov.web.models.ApportionRequestV2;
import org.egov.web.models.Bill;
import org.egov.web.models.BillAccountDetail;
import org.egov.web.models.BillDetail;
import org.egov.web.models.Demand;
import org.egov.web.models.DemandDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private TaxHeadMasterService taxHeadMasterService;

    @InjectMocks
    private TranslationService translationService;

    @Test
    void translate_bill_mapsTaxDetails() {
        BillAccountDetail account = BillAccountDetail.builder()
                .id("acc-1")
                .amount(BigDecimal.TEN)
                .taxHeadCode("TAX1")
                .order(1)
                .build();
        BillDetail detail = BillDetail.builder()
                .id("bd-1")
                .fromPeriod(1L)
                .amount(BigDecimal.TEN)
                .billAccountDetails(Collections.singletonList(account))
                .build();
        Bill bill = Bill.builder()
                .businessService("WS")
                .amountPaid(BigDecimal.TEN)
                .isAdvanceAllowed(false)
                .billDetails(Collections.singletonList(detail))
                .build();

        ApportionRequestV2 result = translationService.translate(bill);

        assertThat(result.getTaxDetails()).hasSize(1);
        assertThat(result.getTaxDetails().get(0).getBuckets()).hasSize(1);
    }

    @Test
    void translate_demands_buildsApportionRequest() {
        when(taxHeadMasterService.getCodeToOrderMap("WS", null)).thenReturn(Map.of("TAX1", 1));
        when(taxHeadMasterService.isAdvanceAllowed("WS", null)).thenReturn(true);

        DemandDetail demandDetail = DemandDetail.builder()
                .id("dd-1")
                .taxHeadMasterCode("TAX1")
                .taxAmount(BigDecimal.TEN)
                .collectionAmount(BigDecimal.ONE)
                .build();
        Demand demand = Demand.builder()
                .id("d-1")
                .businessService("WS")
                .taxPeriodFrom(1L)
                .demandDetails(Collections.singletonList(demandDetail))
                .build();

        ApportionRequestV2 result = translationService.translate(Collections.singletonList(demand), null);

        assertThat(result.getBusinessService()).isEqualTo("WS");
        assertThat(result.getIsAdvanceAllowed()).isTrue();
    }

    @Test
    void translate_demands_invalidTaxHead_throws() {
        when(taxHeadMasterService.getCodeToOrderMap("WS", null)).thenReturn(Map.of());
        when(taxHeadMasterService.isAdvanceAllowed("WS", null)).thenReturn(false);

        DemandDetail demandDetail = DemandDetail.builder()
                .id("dd-1")
                .taxHeadMasterCode("UNKNOWN")
                .taxAmount(BigDecimal.TEN)
                .build();
        Demand demand = Demand.builder()
                .businessService("WS")
                .taxPeriodFrom(1L)
                .demandDetails(Collections.singletonList(demandDetail))
                .build();

        assertThatThrownBy(() -> translationService.translate(Collections.singletonList(demand), null))
                .isInstanceOf(CustomException.class);
    }
}
