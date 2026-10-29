package org.egov.receipt.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.egov.receipt.consumer.FinanceConsumerTestFixtures;
import org.egov.receipt.consumer.model.DishonorReasonContract;
import org.egov.receipt.consumer.model.FinancialStatus;
import org.egov.receipt.consumer.model.InstrumentContract;
import org.egov.receipt.consumer.model.InstrumentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InstrumentUpdateConsumerTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private InstrumentService instrumentService;
    @InjectMocks
    private InstrumentUpdateConsumer consumer;

    @Test
    void process_dishonorUpdate_triggersInstrumentService() throws Exception {
        InstrumentContract contract = InstrumentContract.builder()
                .id("inst-1")
                .financialStatus(FinancialStatus.builder().code("Dishonored").build())
                .dishonor(DishonorReasonContract.builder().dishonorDate(FinanceConsumerTestFixtures.RECEIPT_DATE).build())
                .build();
        InstrumentRequest request = new InstrumentRequest();
        request.setInstruments(List.of(contract));
        request.setRequestInfo(FinanceConsumerTestFixtures.requestInfo());
        request.getRequestInfo().setAction("update");

        Map<String, Object> payload = new HashMap<>();
        payload.put("instrument_persisted", request);
        String json = objectMapper.writeValueAsString(payload);

        consumer.process(new ConsumerRecord<>("egf.instrument.completed", 0, 0L, "k", json));

        verify(instrumentService).processDishonorIntruments(any());
    }

    @Test
    void process_nonUpdateAction_ignored() throws Exception {
        InstrumentContract contract = InstrumentContract.builder()
                .financialStatus(FinancialStatus.builder().code("Dishonored").build())
                .dishonor(DishonorReasonContract.builder().dishonorDate(1L).build())
                .build();
        InstrumentRequest request = new InstrumentRequest();
        request.setInstruments(List.of(contract));
        request.setRequestInfo(FinanceConsumerTestFixtures.requestInfo());
        request.getRequestInfo().setAction("create");

        Map<String, Object> payload = Map.of("instrument_persisted", request);
        consumer.process(new ConsumerRecord<>("egf.instrument.completed", 0, 0L, "k", objectMapper.writeValueAsString(payload)));

        verify(instrumentService, never()).processDishonorIntruments(any());
    }

    @Test
    void process_invalidPayload_swallowed() throws Exception {
        consumer.process(new ConsumerRecord<>("egf.instrument.completed", 0, 0L, "k", "{bad"));

        verify(instrumentService, never()).processDishonorIntruments(any());
    }
}
