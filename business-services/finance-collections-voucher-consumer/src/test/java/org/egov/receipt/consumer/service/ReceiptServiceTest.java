package org.egov.receipt.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.User;
import org.egov.receipt.consumer.model.ReceiptResponse;
import org.egov.receipt.consumer.model.Voucher;
import org.egov.receipt.consumer.model.VoucherResponse;
import org.egov.receipt.consumer.repository.ServiceRequestRepository;
import org.egov.receipt.consumer.support.ReceiptTestFixtures;
import org.egov.reciept.consumer.config.PropertiesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
        var req = ReceiptTestFixtures.receiptReqFromFixture();
        Voucher voucher = new Voucher();
        voucher.setVoucherNumber("VCH-100");
        VoucherResponse voucherResponse = new VoucherResponse();
        voucherResponse.setVouchers(List.of(voucher));
        User siUser = new User();
        siUser.setUuid("si-user");
        when(propertiesManager.getSiUserInfo()).thenReturn(siUser);
        when(serviceRequestRepository.fetchResult(any(StringBuilder.class), any(), anyString()))
                .thenReturn(Map.of("Receipt", List.of()));

        ReceiptResponse response = receiptService.updateReceipt(req, voucherResponse);

        assertThat(response).isNotNull();
        assertThat(req.getReceipt().get(0).getBill().get(0).getBillDetails().get(0).getVoucherHeader())
                .isEqualTo("VCH-100");
        assertThat(req.getRequestInfo().getUserInfo()).isSameAs(siUser);

        ArgumentCaptor<StringBuilder> urlCaptor = ArgumentCaptor.forClass(StringBuilder.class);
        verify(serviceRequestRepository).fetchResult(urlCaptor.capture(), eq(req), eq("pb.amritsar"));
        assertThat(urlCaptor.getValue().toString()).isEqualTo("http://collection/receipts/_update");
    }
}
