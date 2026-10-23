package org.egov.receipt.consumer.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.egov.receipt.consumer.repository.VoucherIntegartionLogRepository;
import org.egov.receipt.consumer.util.PaymentUtils;
import org.egov.reciept.consumer.config.PropertiesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EgfKafkaListenerTest {

    @Mock
    private VoucherService voucherService;
    @Mock
    private ReceiptService receiptService;
    @Mock
    private InstrumentService instrumentService;
    @Mock
    private PropertiesManager manager;
    @Mock
    private VoucherIntegartionLogRepository logRepository;
    @Mock
    private PaymentUtils payUtils;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private EgfKafkaListener listener;

    @BeforeEach
    void setUp() throws Exception {
        when(objectMapper.readValue(any(String.class), any(Class.class)))
                .thenThrow(new RuntimeException("invalid payload"));
    }

    @Test
    void process_invalidPayload_doesNotCreateVoucher() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, "key", "not-json");

        listener.process(record);

        verify(voucherService, never()).createReceiptVoucher(any(), any(), any());
    }
}
