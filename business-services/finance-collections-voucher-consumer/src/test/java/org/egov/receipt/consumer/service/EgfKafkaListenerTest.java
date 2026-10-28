package org.egov.receipt.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.egov.receipt.consumer.entity.VoucherIntegrationLog;
import org.egov.receipt.consumer.model.EgwStatus;
import org.egov.receipt.consumer.model.ProcessStatus;
import org.egov.receipt.consumer.model.ReceiptReq;
import org.egov.receipt.consumer.model.Voucher;
import org.egov.receipt.consumer.model.VoucherResponse;
import org.egov.receipt.consumer.repository.VoucherIntegartionLogRepository;
import org.egov.receipt.consumer.support.ReceiptTestFixtures;
import org.egov.receipt.consumer.util.PaymentUtils;
import org.egov.reciept.consumer.config.PropertiesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EgfKafkaListenerTest {

    private static final String VOUCHER_CREATE_TOPIC = "egov.collection.receipt.voucher.save";
    private static final String VOUCHER_CANCEL_TOPIC = "egov.collection.receipt.voucher.cancel";

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

    @InjectMocks
    private EgfKafkaListener listener;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(listener, "objectMapper", objectMapper);
        when(manager.getVoucherCreateTopic()).thenReturn(VOUCHER_CREATE_TOPIC);
        when(manager.getVoucherCancelTopic()).thenReturn(VOUCHER_CANCEL_TOPIC);
        when(manager.getCreatePaymentTopicName()).thenReturn("payment.create");
        when(manager.getCancelPaymentTopicName()).thenReturn("payment.cancel");
    }

    @Test
    void process_invalidPayload_doesNotCreateVoucher() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(VOUCHER_CREATE_TOPIC, 0, 0L, "key", "not-json");

        listener.process(record);

        verify(voucherService, never()).createReceiptVoucher(any(), any(), any());
        verify(logRepository, never()).saveVoucherIntegrationLog(any());
    }

    @Test
    void process_tenantNotEnabled_skipsVoucherProcessing() throws Exception {
        ReceiptReq req = ReceiptTestFixtures.receiptReqForVoucherCreation("pb.amritsar", "PT");
        ConsumerRecord<String, String> record = receiptRecord(VOUCHER_CREATE_TOPIC, req);
        when(voucherService.isTenantEnabledInFinanceModule(any(), any())).thenReturn(false);

        listener.process(record);

        verify(voucherService, never()).createReceiptVoucher(any(), any(), any());
        verify(logRepository, never()).saveVoucherIntegrationLog(any());
    }

    @Test
    void process_voucherCreateTopic_createsVoucherUpdatesReceiptAndLogsSuccess() throws Exception {
        ReceiptReq req = ReceiptTestFixtures.receiptReqForVoucherCreation("pb.amritsar", "PT");
        ConsumerRecord<String, String> record = receiptRecord(VOUCHER_CREATE_TOPIC, req);
        stubTenantEnabled();
        when(voucherService.getVoucherByServiceAndRefDoc(any(), eq("pb.amritsar"), eq("PT"), eq("RCPT-001")))
                .thenReturn(emptyVoucherResponse());
        when(voucherService.isVoucherCreationEnabled(any(), any(), any())).thenReturn(true);
        VoucherResponse created = voucherResponseWithNumber("VCH-NEW");
        when(voucherService.createReceiptVoucher(any(), any(), isNull())).thenReturn(created);

        listener.process(record);

        verify(receiptService).updateReceipt(any(), eq(created));
        verify(instrumentService).createInstrument(any(), eq(created), any(), isNull());
        ArgumentCaptor<VoucherIntegrationLog> logCaptor = ArgumentCaptor.forClass(VoucherIntegrationLog.class);
        verify(logRepository).saveVoucherIntegrationLog(logCaptor.capture());
        VoucherIntegrationLog log = logCaptor.getValue();
        assertThat(log.getStatus()).isEqualTo(ProcessStatus.SUCCESS.name());
        assertThat(log.getDescription()).contains("VCH-NEW");
        assertThat(log.getVoucherNumber()).isEqualTo("VCH-NEW");
    }

    @Test
    void process_voucherCreateTopic_existingActiveVoucher_logsNaWithoutCreating() throws Exception {
        ReceiptReq req = ReceiptTestFixtures.receiptReqForVoucherCreation("pb.amritsar", "PT");
        ConsumerRecord<String, String> record = receiptRecord(VOUCHER_CREATE_TOPIC, req);
        stubTenantEnabled();
        Voucher existing = Voucher.builder()
                .voucherNumber("VCH-EXISTING")
                .status(EgwStatus.builder().code("1").build())
                .build();
        when(voucherService.getVoucherByServiceAndRefDoc(any(), any(), any(), any()))
                .thenReturn(voucherResponseWith(existing));
        when(voucherService.isVoucherCreationEnabled(any(), any(), any())).thenReturn(true);

        listener.process(record);

        verify(voucherService, never()).createReceiptVoucher(any(), any(), any());
        ArgumentCaptor<VoucherIntegrationLog> logCaptor = ArgumentCaptor.forClass(VoucherIntegrationLog.class);
        verify(logRepository).saveVoucherIntegrationLog(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(ProcessStatus.NA.name());
        assertThat(logCaptor.getValue().getDescription()).contains("Already voucher exists");
    }

    @Test
    void process_voucherCreateTopic_voucherCreationDisabled_logsSuccessWithoutCreate() throws Exception {
        ReceiptReq req = ReceiptTestFixtures.receiptReqForVoucherCreation("pb.amritsar", "PT");
        ConsumerRecord<String, String> record = receiptRecord(VOUCHER_CREATE_TOPIC, req);
        stubTenantEnabled();
        when(voucherService.getVoucherByServiceAndRefDoc(any(), any(), any(), any()))
                .thenReturn(emptyVoucherResponse());
        when(voucherService.isVoucherCreationEnabled(any(), any(), any())).thenReturn(false);

        listener.process(record);

        verify(voucherService, never()).createReceiptVoucher(any(), any(), any());
        ArgumentCaptor<VoucherIntegrationLog> logCaptor = ArgumentCaptor.forClass(VoucherIntegrationLog.class);
        verify(logRepository).saveVoucherIntegrationLog(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(ProcessStatus.SUCCESS.name());
        assertThat(logCaptor.getValue().getDescription()).contains("not enabled");
    }

    @Test
    void process_voucherCancelTopic_noVoucherInErp_logsFailedBackup() throws Exception {
        ReceiptReq req = ReceiptTestFixtures.receiptReqFromFixture();
        ConsumerRecord<String, String> record = receiptRecord(VOUCHER_CANCEL_TOPIC, req);
        stubTenantEnabled();
        when(voucherService.getVoucherByServiceAndRefDoc(any(), any(), any(), any()))
                .thenReturn(emptyVoucherResponse());

        listener.process(record);

        verify(voucherService, never()).cancelReceiptVoucher(any(), any(), anySet());
        ArgumentCaptor<VoucherIntegrationLog> logCaptor = ArgumentCaptor.forClass(VoucherIntegrationLog.class);
        verify(logRepository).saveVoucherIntegrationLog(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(ProcessStatus.FAILED.name());
        assertThat(logCaptor.getValue().getDescription()).contains("Voucher is not exist");
    }

    @Test
    void process_voucherCancelTopic_voucherAlreadyCancelled_logsNaWithoutCancel() throws Exception {
        ReceiptReq req = ReceiptTestFixtures.receiptReqFromFixture();
        ConsumerRecord<String, String> record = receiptRecord(VOUCHER_CANCEL_TOPIC, req);
        stubTenantEnabled();
        Voucher cancelled = Voucher.builder()
                .voucherNumber("VCH-CANCELLED")
                .status(EgwStatus.builder().code("4").build())
                .build();
        when(voucherService.getVoucherByServiceAndRefDoc(any(), any(), any(), any()))
                .thenReturn(voucherResponseWith(cancelled));

        listener.process(record);

        verify(voucherService, never()).cancelReceiptVoucher(any(), any(), anySet());
        ArgumentCaptor<VoucherIntegrationLog> logCaptor = ArgumentCaptor.forClass(VoucherIntegrationLog.class);
        verify(logRepository).saveVoucherIntegrationLog(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(ProcessStatus.NA.name());
        assertThat(logCaptor.getValue().getDescription()).contains("already in Cancelled status");
    }

    @Test
    void process_voucherCancelTopic_activeVoucher_cancelsVoucherAndInstrument() throws Exception {
        ReceiptReq req = ReceiptTestFixtures.receiptReqForVoucherCreation("pb.amritsar", "PT");
        req.getReceipt().get(0).getBill().get(0).getBillDetails().get(0).setVoucherHeader("VCH-ACTIVE");
        ConsumerRecord<String, String> record = receiptRecord(VOUCHER_CANCEL_TOPIC, req);
        stubTenantEnabled();
        Voucher active = Voucher.builder()
                .voucherNumber("VCH-ACTIVE")
                .status(EgwStatus.builder().code("2").build())
                .build();
        when(voucherService.getVoucherByServiceAndRefDoc(any(), any(), any(), any()))
                .thenReturn(voucherResponseWith(active));

        listener.process(record);

        verify(voucherService).cancelReceiptVoucher(any(), eq("pb.amritsar"), eq(Collections.singleton("VCH-ACTIVE")));
        verify(instrumentService).cancelInstrument(any(), any());
        ArgumentCaptor<VoucherIntegrationLog> logCaptor = ArgumentCaptor.forClass(VoucherIntegrationLog.class);
        verify(logRepository).saveVoucherIntegrationLog(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(ProcessStatus.SUCCESS.name());
        assertThat(logCaptor.getValue().getDescription()).contains("CANCELLED successfully");
    }

    private void stubTenantEnabled() throws Exception {
        when(voucherService.isTenantEnabledInFinanceModule(any(), any())).thenReturn(true);
    }

    private ConsumerRecord<String, String> receiptRecord(String topic, ReceiptReq req) throws Exception {
        return new ConsumerRecord<>(topic, 0, 0L, "key", objectMapper.writeValueAsString(req));
    }

    private static VoucherResponse emptyVoucherResponse() {
        VoucherResponse response = new VoucherResponse();
        response.setVouchers(new ArrayList<>());
        return response;
    }

    private static VoucherResponse voucherResponseWith(Voucher voucher) {
        VoucherResponse response = new VoucherResponse();
        response.setVouchers(new ArrayList<>(List.of(voucher)));
        return response;
    }

    private static VoucherResponse voucherResponseWithNumber(String voucherNumber) {
        return voucherResponseWith(Voucher.builder().voucherNumber(voucherNumber).build());
    }
}
