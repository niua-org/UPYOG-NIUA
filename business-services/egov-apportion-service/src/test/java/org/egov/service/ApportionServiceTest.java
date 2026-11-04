package org.egov.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.config.ApportionConfig;
import org.egov.producer.Producer;
import org.egov.web.models.ApportionRequest;
import org.egov.web.models.Bill;
import org.egov.web.models.BillDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApportionServiceTest {

    @Mock
    private Producer producer;
    @Mock
    private ApportionConfig config;
    @Mock
    private MDMSService mdmsService;

    @Test
    void apportionBills_usesDefaultApportionStrategy() {
        Apportion apportion = mock(Apportion.class);
        when(apportion.getBusinessService()).thenReturn("DEFAULT");
        when(config.getBillRequestTopic()).thenReturn("req-topic");
        when(config.getBillResponseTopic()).thenReturn("res-topic");
        when(mdmsService.mDMSCall(any(), any())).thenReturn(new Object());

        BillDetail billDetail = BillDetail.builder().fromPeriod(1L).build();
        Bill bill = Bill.builder()
                .businessService("WS")
                .amountPaid(BigDecimal.TEN)
                .billDetails(Collections.singletonList(billDetail))
                .build();
        ApportionRequest request = ApportionRequest.builder()
                .requestInfo(new RequestInfo())
                .tenantId("pb")
                .bills(Collections.singletonList(bill))
                .build();

        ApportionService service = new ApportionService(List.of(apportion), producer, config, mdmsService);
        List<Bill> result = service.apportionBills(request);

        assertThat(result).hasSize(1);
        verify(producer).push(eq("req-topic"), eq(request));
        verify(producer).push(eq("res-topic"), eq(request));
        verify(apportion).apportionPaidAmount(eq(bill), any());
    }
}
