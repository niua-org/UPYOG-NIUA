package org.egov.receipt.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.User;
import org.egov.receipt.consumer.FinanceConsumerTestFixtures;
import org.egov.receipt.consumer.model.ReceiptReq;
import org.egov.receipt.consumer.model.ReceiptResponse;
import org.egov.receipt.consumer.model.VoucherResponse;
import org.egov.receipt.consumer.repository.ServiceRequestRepository;
import org.egov.reciept.consumer.config.PropertiesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @Mock
    private PropertiesManager propertiesManager;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Spy
    private ObjectMapper mapper = new ObjectMapper();
    @InjectMocks
    private ReceiptService receiptService;

    @BeforeEach
    void setUp() {
        when(propertiesManager.getCollectionsHostUrl()).thenReturn("http://collections/");
        when(propertiesManager.getReceiptsUpdate()).thenReturn("/receipt/update");
        when(propertiesManager.getSiUserInfo()).thenReturn(User.builder().uuid("u1").build());
    }

    @Test
    void updateReceipt_setsVoucherHeaderAndUserInfo() throws Exception {
        ReceiptReq receiptReq = FinanceConsumerTestFixtures.receiptReq("RCPT-001", null, null);
        VoucherResponse voucherResponse = FinanceConsumerTestFixtures.voucherResponse("VN-500", "0");
        ReceiptResponse apiResponse = new ReceiptResponse();
        when(serviceRequestRepository.fetchResult(any(), any(), eq(FinanceConsumerTestFixtures.TENANT_ID)))
                .thenReturn(apiResponse);

        ReceiptResponse result = receiptService.updateReceipt(receiptReq, voucherResponse);

        assertThat(result).isNotNull();
        assertThat(receiptReq.getReceipt().get(0).getBill().get(0).getBillDetails().get(0).getVoucherHeader())
                .isEqualTo("VN-500");
        assertThat(receiptReq.getRequestInfo().getUserInfo().getUuid()).isEqualTo("u1");
    }
}
