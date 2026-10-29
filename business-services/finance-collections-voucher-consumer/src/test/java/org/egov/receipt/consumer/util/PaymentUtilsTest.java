package org.egov.receipt.consumer.util;

import org.egov.receipt.consumer.model.Receipt;
import org.egov.receipt.consumer.repository.ServiceRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentUtilsTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @InjectMocks
    private PaymentUtils paymentUtils;

    @Test
    void getReceiptsFromPayments_emptyInput_leavesReceiptListEmpty() throws Exception {
        List<Receipt> receipts = new ArrayList<>();

        paymentUtils.getReceiptsFromPayments(Collections.emptyList(), receipts);

        assertThat(receipts).isEmpty();
    }
}
