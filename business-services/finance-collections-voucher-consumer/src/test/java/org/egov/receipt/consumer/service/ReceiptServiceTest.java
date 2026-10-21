package org.egov.receipt.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.receipt.consumer.model.ReceiptResponse;
import org.egov.receipt.consumer.model.Voucher;
import org.egov.receipt.consumer.model.VoucherResponse;
import org.egov.receipt.consumer.repository.ServiceRequestRepository;
import org.egov.receipt.consumer.support.ReceiptTestFixtures;
import org.egov.reciept.consumer.config.PropertiesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @Mock
    private PropertiesManager propertiesManager;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @InjectMocks
    private ReceiptService receiptService;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(receiptService, "mapper", new ObjectMapper());
        when(propertiesManager.getCollectionsHostUrl()).thenReturn("http://collection/");
        when(propertiesManager.getReceiptsUpdate()).thenReturn("receipts/_update");
    }

    @Test
    void updateReceipt_setsVoucherHeaderAndCallsCollection() throws Exception {
        var req = ReceiptTestFixtures.receiptReq("pb.amritsar", "PT");
        Voucher voucher = new Voucher();
        voucher.setVoucherNumber("VCH-100");
        VoucherResponse voucherResponse = new VoucherResponse();
        voucherResponse.setVouchers(List.of(voucher));

        when(serviceRequestRepository.fetchResult(any(StringBuilder.class), any(), anyString()))
                .thenReturn(Map.of("Receipt", List.of()));

        ReceiptResponse response = receiptService.updateReceipt(req, voucherResponse);

        assertThat(response).isNotNull();
        assertThat(req.getReceipt().get(0).getBill().get(0).getBillDetails().get(0).getVoucherHeader())
                .isEqualTo("VCH-100");
    }
}
