package org.egov.receipt.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.egov.receipt.consumer.FinanceConsumerTestFixtures;
import org.egov.receipt.consumer.model.ProcessStatus;
import org.egov.receipt.consumer.model.ReceiptReq;
import org.egov.receipt.consumer.model.VoucherResponse;
import org.egov.receipt.consumer.repository.VoucherIntegartionLogRepository;
import org.egov.receipt.consumer.util.PaymentUtils;
import org.egov.receipt.consumer.v2.model.PaymentRequest;
import org.egov.receipt.consumer.v2.model.PaymentStatusEnum;
import org.egov.receipt.custom.exception.VoucherCustomException;
import org.egov.reciept.consumer.config.PropertiesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EgfKafkaListenerTest {

    private static final String CREATE_TOPIC = "egov.collection.receipt.voucher.save";
    private static final String CANCEL_TOPIC = "egov.collection.receipt.voucher.cancel";
    private static final String PAY_CREATE = "kafka.payment.create";
    private static final String PAY_CANCEL = "kafka.payment.cancel";

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private VoucherService voucherService;
    @Mock
    private ReceiptService receiptService;
    @Mock
    private InstrumentService instrumentService;
    @Mock
    private VoucherIntegartionLogRepository voucherIntegartionLogRepository;
    @Mock
    private PropertiesManager manager;
    @Mock
    private PaymentUtils payUtils;
    @InjectMocks
    private EgfKafkaListener listener;

    @BeforeEach
    void topics() {
        when(manager.getVoucherCreateTopic()).thenReturn(CREATE_TOPIC);
        when(manager.getVoucherCancelTopic()).thenReturn(CANCEL_TOPIC);
        when(manager.getCreatePaymentTopicName()).thenReturn(PAY_CREATE);
        when(manager.getCancelPaymentTopicName()).thenReturn(PAY_CANCEL);
    }

    @Test
    void process_voucherCreate_successFlow() throws Exception {
        ReceiptReq receiptReq = FinanceConsumerTestFixtures.receiptReq("RCPT-001", null, null);
        String payload = objectMapper.writeValueAsString(receiptReq);
        when(voucherService.isTenantEnabledInFinanceModule(any(), any())).thenReturn(true);
        when(voucherService.getVoucherByServiceAndRefDoc(any(), any(), any(), any()))
                .thenReturn(VoucherResponse.builder().vouchers(Collections.emptyList()).build());
        when(voucherService.isVoucherCreationEnabled(any(), any(), any())).thenReturn(true);
        when(voucherService.createReceiptVoucher(any(), any(), eq(null)))
                .thenReturn(FinanceConsumerTestFixtures.voucherResponse("VN-1", "0"));

        listener.process(new ConsumerRecord<>(CREATE_TOPIC, 0, 0L, "k", payload));

        verify(receiptService).updateReceipt(any(), any());
        verify(instrumentService).createInstrument(any(), any(), any(), eq(null));
        verify(voucherIntegartionLogRepository).saveVoucherIntegrationLog(any());
    }

    @Test
    void process_voucherCreate_disabled_logsSuccessWithoutCreate() throws Exception {
        ReceiptReq receiptReq = FinanceConsumerTestFixtures.receiptReq("RCPT-001", null, null);
        when(voucherService.isTenantEnabledInFinanceModule(any(), any())).thenReturn(true);
        when(voucherService.getVoucherByServiceAndRefDoc(any(), any(), any(), any()))
                .thenReturn(VoucherResponse.builder().vouchers(Collections.emptyList()).build());
        when(voucherService.isVoucherCreationEnabled(any(), any(), any())).thenReturn(false);

        listener.process(new ConsumerRecord<>(CREATE_TOPIC, 0, 0L, "k", objectMapper.writeValueAsString(receiptReq)));

        verify(voucherService, never()).createReceiptVoucher(any(), any(), any());
        verify(voucherIntegartionLogRepository).saveVoucherIntegrationLog(any());
    }

    @Test
    void process_voucherCreate_existingVoucher_recordsNa() throws Exception {
        ReceiptReq receiptReq = FinanceConsumerTestFixtures.receiptReq("RCPT-001", null, null);
        when(voucherService.isTenantEnabledInFinanceModule(any(), any())).thenReturn(true);
        when(voucherService.getVoucherByServiceAndRefDoc(any(), any(), any(), any()))
                .thenReturn(FinanceConsumerTestFixtures.voucherResponse("VN-EXIST", "0"));
        when(voucherService.isVoucherCreationEnabled(any(), any(), any())).thenReturn(true);

        listener.process(new ConsumerRecord<>(CREATE_TOPIC, 0, 0L, "k", objectMapper.writeValueAsString(receiptReq)));

        verify(voucherService, never()).createReceiptVoucher(any(), any(), any());
        verify(voucherIntegartionLogRepository).saveVoucherIntegrationLog(any());
    }

    @Test
    void process_voucherCancel_notFound_recordsFailure() throws Exception {
        ReceiptReq receiptReq = FinanceConsumerTestFixtures.receiptReq("RCPT-001", "VN-REQ", null);
        when(voucherService.isTenantEnabledInFinanceModule(any(), any())).thenReturn(true);
        when(voucherService.getVoucherByServiceAndRefDoc(any(), any(), any(), any()))
                .thenReturn(VoucherResponse.builder().vouchers(Collections.emptyList()).build());

        listener.process(new ConsumerRecord<>(CANCEL_TOPIC, 0, 0L, "k", objectMapper.writeValueAsString(receiptReq)));

        verify(voucherIntegartionLogRepository).saveVoucherIntegrationLog(any());
    }

    @Test
    void process_voucherCancel_alreadyCancelled_recordsNa() throws Exception {
        ReceiptReq receiptReq = FinanceConsumerTestFixtures.receiptReq("RCPT-001", "VN-REQ", null);
        when(voucherService.isTenantEnabledInFinanceModule(any(), any())).thenReturn(true);
        when(voucherService.getVoucherByServiceAndRefDoc(any(), any(), any(), any()))
                .thenReturn(FinanceConsumerTestFixtures.voucherResponse("VN-ERP", "4"));

        listener.process(new ConsumerRecord<>(CANCEL_TOPIC, 0, 0L, "k", objectMapper.writeValueAsString(receiptReq)));

        verify(voucherService, never()).cancelReceiptVoucher(any(), any(), any());
        verify(voucherIntegartionLogRepository).saveVoucherIntegrationLog(any());
    }

    @Test
    void process_voucherCancel_activeVoucher_cancels() throws Exception {
        ReceiptReq receiptReq = FinanceConsumerTestFixtures.receiptReq("RCPT-001", "VN-REQ", null);
        when(voucherService.isTenantEnabledInFinanceModule(any(), any())).thenReturn(true);
        when(voucherService.getVoucherByServiceAndRefDoc(any(), any(), any(), any()))
                .thenReturn(FinanceConsumerTestFixtures.voucherResponse("VN-ERP", "0"));

        listener.process(new ConsumerRecord<>(CANCEL_TOPIC, 0, 0L, "k", objectMapper.writeValueAsString(receiptReq)));

        verify(voucherService).cancelReceiptVoucher(any(), any(), eq(Set.of("VN-ERP")));
        verify(instrumentService).cancelInstrument(any(), any());
    }

    @Test
    void process_paymentCreate_transformsAndCreatesVoucher() throws Exception {
        PaymentRequest paymentRequest = FinanceConsumerTestFixtures.paymentRequest(null);
        when(voucherService.isTenantEnabledInFinanceModule(any(), any())).thenReturn(true);
        doAnswer(invocation -> {
            invocation.<java.util.List>getArgument(1).add(FinanceConsumerTestFixtures.receiptReq("RCPT-PAY", null, null).getReceipt().get(0));
            return null;
        }).when(payUtils).getReceiptsFromPayments(any(), any());
        when(voucherService.getVoucherByServiceAndRefDoc(any(), any(), any(), any()))
                .thenReturn(VoucherResponse.builder().vouchers(Collections.emptyList()).build());
        when(voucherService.isVoucherCreationEnabled(any(), any(), any())).thenReturn(true);
        when(voucherService.createReceiptVoucher(any(), any(), anyString()))
                .thenReturn(FinanceConsumerTestFixtures.voucherResponse("VN-PAY", "0"));

        listener.process(new ConsumerRecord<>(PAY_CREATE, 0, 0L, "k", objectMapper.writeValueAsString(paymentRequest)));

        verify(instrumentService).createInstrument(any(), any(), any(), eq("V2"));
        verify(voucherIntegartionLogRepository).saveVoucherIntegrationLog(any());
    }

    @Test
    void process_paymentCancel_success() throws Exception {
        PaymentRequest paymentRequest = FinanceConsumerTestFixtures.paymentRequest(PaymentStatusEnum.CANCELLED);
        when(voucherService.isTenantEnabledInFinanceModule(any(), any())).thenReturn(true);
        doAnswer(invocation -> {
            invocation.<java.util.List>getArgument(1).add(FinanceConsumerTestFixtures.receiptReq("RCPT-PAY", null, null).getReceipt().get(0));
            return null;
        }).when(payUtils).getReceiptsFromPayments(any(), any());
        when(voucherService.getVoucherByServiceAndRefDoc(any(), any(), any(), any()))
                .thenReturn(FinanceConsumerTestFixtures.voucherResponse("VN-PAY", "0"));

        listener.process(new ConsumerRecord<>(PAY_CANCEL, 0, 0L, "k", objectMapper.writeValueAsString(paymentRequest)));

        verify(voucherService).cancelReceiptVoucher(any(), any(), any());
        verify(instrumentService).cancelInstrumentForPayment(any(), any());
    }

    @Test
    void process_paymentCancel_dishonoured_onlyLogs() throws Exception {
        PaymentRequest paymentRequest = FinanceConsumerTestFixtures.paymentRequest(PaymentStatusEnum.DISHONOURED);
        doAnswer(invocation -> {
            invocation.<java.util.List>getArgument(1).add(FinanceConsumerTestFixtures.receiptReq("RCPT-PAY", null, null).getReceipt().get(0));
            return null;
        }).when(payUtils).getReceiptsFromPayments(any(), any());
        when(voucherService.isTenantEnabledInFinanceModule(any(), any())).thenReturn(true);

        listener.process(new ConsumerRecord<>(PAY_CANCEL, 0, 0L, "k", objectMapper.writeValueAsString(paymentRequest)));

        verify(voucherService, never()).cancelReceiptVoucher(any(), any(), any());
    }

    @Test
    void process_tenantNotEnabled_recordsCustomException() throws Exception {
        ReceiptReq receiptReq = FinanceConsumerTestFixtures.receiptReq("RCPT-001", null, null);
        doThrow(new VoucherCustomException(ProcessStatus.FAILED, "tenant disabled"))
                .when(voucherService).isTenantEnabledInFinanceModule(any(), any());

        listener.process(new ConsumerRecord<>(CREATE_TOPIC, 0, 0L, "k", objectMapper.writeValueAsString(receiptReq)));

        verify(voucherIntegartionLogRepository).saveVoucherIntegrationLog(any());
    }

    @Test
    void process_invalidJson_doesNotPersistWhenReceiptMissing() {
        listener.process(new ConsumerRecord<>(CREATE_TOPIC, 0, 0L, "k", "{not-json"));

        verify(voucherIntegartionLogRepository, never()).saveVoucherIntegrationLog(any());
    }
}
