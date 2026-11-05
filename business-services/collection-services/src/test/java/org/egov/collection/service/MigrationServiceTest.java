package org.egov.collection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.collection.config.ApplicationProperties;
import org.egov.collection.model.Instrument;
import org.egov.collection.model.InstrumentType;
import org.egov.collection.model.Payment;
import org.egov.collection.model.PaymentResponse;
import org.egov.collection.model.enums.InstrumentStatusEnum;
import org.egov.collection.model.enums.PaymentModeEnum;
import org.egov.collection.model.enums.PaymentStatusEnum;
import org.egov.collection.model.enums.Purpose;
import org.egov.collection.model.enums.ReceiptStatus;
import org.egov.collection.model.v1.AuditDetails_v1;
import org.egov.collection.model.v1.BillAccountDetail_v1;
import org.egov.collection.model.v1.BillDetail_v1;
import org.egov.collection.model.v1.Bill_v1;
import org.egov.collection.model.v1.ReceiptSearchCriteria_v1;
import org.egov.collection.model.v1.Receipt_v1;
import org.egov.collection.producer.CollectionProducer;
import org.egov.collection.repository.ServiceRequestRepository;
import org.egov.collection.service.v1.CollectionService_v1;
import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MigrationServiceTest {

    @Mock
    private CollectionProducer collectionProducer;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private CollectionService_v1 collectionService;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private ApplicationProperties applicationProperties;
    private MigrationService migrationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        applicationProperties = new ApplicationProperties();
        applicationProperties.setCollectionMigrationTopicName("collection-migration");
        applicationProperties.setBillingServiceHostName("http://billing");
        applicationProperties.setSearchBill("/bill/v2/_search");

        migrationService = new MigrationService(applicationProperties, serviceRequestRepository, collectionProducer);
        ReflectionTestUtils.setField(migrationService, "collectionService", collectionService);
        ReflectionTestUtils.setField(migrationService, "jdbcTemplate", jdbcTemplate);
        ReflectionTestUtils.setField(migrationService, "mapper", objectMapper);
        doNothing().when(collectionProducer).producer(anyString(), any());
    }

    @Test
    void migrateReceipt_emptyList_stillPublishesToKafka() {
        migrationService.migrateReceipt(new RequestInfo(), Collections.emptyList());

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(collectionProducer).producer(eq("collection-migration"), payloadCaptor.capture());
        PaymentResponse response = (PaymentResponse) payloadCaptor.getValue();
        assertThat(response.getPayments()).isEmpty();
    }

    @Test
    void migrateReceipt_cashPayment_mapsPaymentWithNewStatus() {
        Receipt_v1 receipt = sampleReceipt("ACTIVE", "CASH", null);

        migrationService.migrateReceipt(new RequestInfo(), List.of(receipt));

        Payment payment = capturePublishedPayment();
        assertThat(payment.getPaymentMode()).isEqualTo(PaymentModeEnum.CASH);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatusEnum.NEW);
        assertThat(payment.getInstrumentStatus()).isEqualTo(InstrumentStatusEnum.APPROVED);
        assertThat(payment.getMobileNumber()).isEqualTo("9999007890");
        assertThat(payment.getPaymentDetails()).hasSize(1);
        assertThat(payment.getPaymentDetails().get(0).getBill().getBillNumber()).isEqualTo("BN-1");
    }

    @Test
    void migrateReceipt_cancelledPayment_marksBillCancelled() {
        Receipt_v1 receipt = sampleReceipt(PaymentStatusEnum.CANCELLED.toString(), "CASH", null);

        migrationService.migrateReceipt(new RequestInfo(), List.of(receipt));

        Payment payment = capturePublishedPayment();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatusEnum.CANCELLED);
        assertThat(payment.getPaymentDetails().get(0).getBill().getIsCancelled()).isTrue();
    }

    @Test
    void migrateReceipt_cardPayment_depositedAndRemitted() {
        Receipt_v1 receipt = sampleReceipt("ACTIVE", "CARD", null);

        migrationService.migrateReceipt(new RequestInfo(), List.of(receipt));

        Payment payment = capturePublishedPayment();
        assertThat(payment.getPaymentMode()).isEqualTo(PaymentModeEnum.CARD);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatusEnum.DEPOSITED);
    }

    @Test
    void migrateReceipt_onlinePayment_depositedAndRemitted() {
        Receipt_v1 receipt = sampleReceipt("ACTIVE", "ONLINE", null);

        migrationService.migrateReceipt(new RequestInfo(), List.of(receipt));

        Payment payment = capturePublishedPayment();
        assertThat(payment.getPaymentMode()).isEqualTo(PaymentModeEnum.ONLINE);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatusEnum.DEPOSITED);
        assertThat(payment.getInstrumentStatus()).isEqualTo(InstrumentStatusEnum.REMITTED);
    }

    @Test
    void migrateReceipt_remittedReceiptStatus_deposited() {
        Receipt_v1 receipt = sampleReceipt(ReceiptStatus.REMITTED.toString(), "CHEQUE", null);

        migrationService.migrateReceipt(new RequestInfo(), List.of(receipt));

        Payment payment = capturePublishedPayment();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatusEnum.DEPOSITED);
    }

    @Test
    void migrateReceipt_nullMobileNumber_defaultsToNa() {
        Receipt_v1 receipt = sampleReceipt("ACTIVE", "CASH", null);
        receipt.getBill().get(0).setMobileNumber(null);

        migrationService.migrateReceipt(new RequestInfo(), List.of(receipt));

        assertThat(capturePublishedPayment().getMobileNumber()).isEqualTo("NA");
    }

    @Test
    void migrateReceipt_manualReceiptDateZero_clearsManualDate() {
        Receipt_v1 receipt = sampleReceipt("ACTIVE", "CASH", 0L);

        migrationService.migrateReceipt(new RequestInfo(), List.of(receipt));

        assertThat(capturePublishedPayment().getPaymentDetails().get(0).getManualReceiptDate()).isNull();
    }

    @Test
    void migrateReceipt_nullBillNumber_defaultsToNaOnBill() {
        Receipt_v1 receipt = sampleReceipt("ACTIVE", "CASH", null);
        receipt.getBill().get(0).getBillDetails().get(0).setBillNumber(null);

        migrationService.migrateReceipt(new RequestInfo(), List.of(receipt));

        assertThat(capturePublishedPayment().getPaymentDetails().get(0).getBill().getBillNumber()).isEqualTo("NA");
    }

    @Test
    void migrate_batchesUntilEmptyResult() throws Exception {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(List.of("pb.amritsar"));
        when(collectionService.fetchReceipts(any(ReceiptSearchCriteria_v1.class)))
                .thenReturn(List.of(sampleReceipt("ACTIVE", "CASH", null)))
                .thenReturn(Collections.emptyList());

        migrationService.migrate(new RequestInfo(), 0, 10, null);

        verify(collectionService, org.mockito.Mockito.atLeastOnce()).fetchReceipts(any(ReceiptSearchCriteria_v1.class));
        verify(collectionProducer, org.mockito.Mockito.atLeastOnce()).producer(eq("collection-migration"), any());
    }

    @Test
    void migrate_filtersSingleTenantWhenProvided() throws Exception {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("pb.amritsar", "pb.patiala"));
        when(collectionService.fetchReceipts(any())).thenReturn(Collections.emptyList());

        migrationService.migrate(new RequestInfo(), 0, 5, "pb.patiala");

        ArgumentCaptor<ReceiptSearchCriteria_v1> criteriaCaptor = ArgumentCaptor.forClass(ReceiptSearchCriteria_v1.class);
        verify(collectionService).fetchReceipts(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().getTenantId()).isEqualTo("pb.patiala");
    }

    @Test
    void getBillFromV2_viaReflection_parsesBillingResponse() throws Exception {
        Map<String, Object> billMap = new HashMap<>();
        billMap.put("id", "bill-1");
        billMap.put("tenantId", "pb");
        Map<String, Object> billResponse = Map.of("Bill", List.of(billMap));
        when(serviceRequestRepository.fetchResult(any(StringBuilder.class), any())).thenReturn(billResponse);

        Receipt_v1 receipt = sampleReceipt("ACTIVE", "CASH", null);
        @SuppressWarnings("unchecked")
        Map<String, org.egov.collection.web.contract.Bill> result =
                (Map<String, org.egov.collection.web.contract.Bill>) ReflectionTestUtils.invokeMethod(
                        migrationService, "getBillsForReceipts", List.of(receipt), new RequestInfo());

        assertThat(result).containsKey("bill-1");
    }

    private Payment capturePublishedPayment() {
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(collectionProducer).producer(eq("collection-migration"), payloadCaptor.capture());
        PaymentResponse response = (PaymentResponse) payloadCaptor.getValue();
        assertThat(response.getPayments()).isNotEmpty();
        return response.getPayments().get(0);
    }

    private Receipt_v1 sampleReceipt(String billStatus, String instrumentTypeName, Long manualReceiptDate) {
        AuditDetails_v1 audit = AuditDetails_v1.builder()
                .createdBy("creator")
                .createdDate(1L)
                .lastModifiedBy("modifier")
                .lastModifiedDate(2L)
                .build();

        BillAccountDetail_v1 account = BillAccountDetail_v1.builder()
                .id("acc-1")
                .tenantId("pb")
                .amount(BigDecimal.valueOf(100))
                .purpose(Purpose.ARREAR)
                .build();

        BillDetail_v1 billDetail = BillDetail_v1.builder()
                .id("bd-1")
                .tenantId("pb")
                .status(billStatus)
                .businessService("PT")
                .consumerCode("CC-1")
                .billNumber("BN-1")
                .billDate(1000L)
                .totalAmount(BigDecimal.valueOf(100))
                .amountPaid(BigDecimal.valueOf(100))
                .manualReceiptNumber("MR-1")
                .manualReceiptDate(manualReceiptDate)
                .receiptType("ADHOC")
                .billAccountDetails(List.of(account))
                .build();

        Bill_v1 bill = Bill_v1.builder()
                .id("bill-1")
                .tenantId("pb")
                .payerName("Payer")
                .payerAddress("Address")
                .payerEmail("p@example.com")
                .payerId("payer-1")
                .paidBy("Payer")
                .mobileNumber("9999007890")
                .additionalDetails(objectMapper.createObjectNode().put("key", "value"))
                .billDetails(List.of(billDetail))
                .build();

        InstrumentType instrumentType = new InstrumentType();
        instrumentType.setName(instrumentTypeName);

        Instrument instrument = Instrument.builder()
                .amount(BigDecimal.valueOf(100))
                .transactionNumber("TXN-1")
                .instrumentDate(2000L)
                .instrumentNumber("INST-1")
                .instrumentStatus(InstrumentStatusEnum.APPROVED)
                .instrumentType(instrumentType)
                .build();

        return Receipt_v1.builder()
                .tenantId("pb")
                .receiptNumber("RCPT-1")
                .receiptDate(3000L)
                .bill(List.of(bill))
                .auditDetails(audit)
                .instrument(instrument)
                .build();
    }
}
