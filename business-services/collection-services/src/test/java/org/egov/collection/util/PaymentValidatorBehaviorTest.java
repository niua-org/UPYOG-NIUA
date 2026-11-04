package org.egov.collection.util;

import org.egov.collection.config.ApplicationProperties;
import org.egov.collection.model.AuditDetails;
import org.egov.collection.model.Payment;
import org.egov.collection.model.PaymentDetail;
import org.egov.collection.model.PaymentRequest;
import org.egov.collection.model.PaymentSearchCriteria;
import org.egov.collection.model.enums.InstrumentStatusEnum;
import org.egov.collection.model.enums.PaymentModeEnum;
import org.egov.collection.model.enums.PaymentStatusEnum;
import org.egov.collection.repository.PaymentRepository;
import org.egov.collection.repository.ServiceRequestRepository;
import org.egov.collection.service.PaymentWorkflowService;
import org.egov.collection.web.contract.Bill;
import org.egov.collection.web.contract.BillDetail;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentValidatorBehaviorTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentWorkflowService paymentWorkflowService;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private PaymentValidator paymentValidator;
    private ApplicationProperties applicationProperties;

    @BeforeEach
    void setUp() {
        applicationProperties = new ApplicationProperties();
        applicationProperties.setReceiptsSearchDefaultLimit(50);
        paymentValidator = new PaymentValidator(paymentRepository, paymentWorkflowService, applicationProperties,
                serviceRequestRepository);
    }

    @Test
    void validatePaymentForCreate_cashPayment_succeeds() {
        when(paymentRepository.fetchPayments(any(PaymentSearchCriteria.class))).thenReturn(List.of());

        PaymentRequest paymentRequest = paymentRequest(PaymentModeEnum.CASH, null, null, null);
        Payment validated = paymentValidator.validatePaymentForCreate(paymentRequest);

        assertThat(validated.getPaymentMode()).isEqualTo(PaymentModeEnum.CASH);
    }

    @Test
    void validatePaymentForCreate_chequeMissingInstrumentDate_collectsError() {
        when(paymentRepository.fetchPayments(any())).thenReturn(List.of());

        PaymentRequest paymentRequest = paymentRequest(PaymentModeEnum.CHEQUE, null, "CHQ-1", null);

        assertThatThrownBy(() -> paymentValidator.validatePaymentForCreate(paymentRequest))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validatePaymentForCreate_onlineMissingTxnNumber_collectsError() {
        when(paymentRepository.fetchPayments(any())).thenReturn(List.of());

        PaymentRequest paymentRequest = paymentRequest(PaymentModeEnum.ONLINE, null, "ONL-1", null);
        paymentRequest.getPayment().setTransactionNumber(null);

        assertThatThrownBy(() -> paymentValidator.validatePaymentForCreate(paymentRequest))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validatePaymentForCreate_existingRemittedPayment_rejectsBill() {
        Payment existing = Payment.builder()
                .instrumentStatus(InstrumentStatusEnum.REMITTED)
                .build();
        when(paymentRepository.fetchPayments(any())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> paymentValidator.validatePaymentForCreate(paymentRequest(PaymentModeEnum.CASH, null, null, null)))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validatePaymentForCreate_existingApprovedPayment_rejectsBill() {
        Payment existing = Payment.builder()
                .instrumentStatus(InstrumentStatusEnum.APPROVED)
                .build();
        when(paymentRepository.fetchPayments(any())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> paymentValidator.validatePaymentForCreate(paymentRequest(PaymentModeEnum.CASH, null, null, null)))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateUserInfo_nullRequestInfo_recordsError() {
        Map<String, String> errors = new HashMap<>();
        paymentValidator.validateUserInfo(null, errors);
        assertThat(errors).containsKey("INVALID_REQUEST_INFO");
    }

    @Test
    void validateUserInfo_missingUuid_recordsError() {
        Map<String, String> errors = new HashMap<>();
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setUserInfo(new User());
        paymentValidator.validateUserInfo(requestInfo, errors);
        assertThat(errors).containsKey("INVALID_USER_ID");
    }

    @Test
    void validatePaymentForCreate_chequeWithValidInstrument_succeeds() {
        when(paymentRepository.fetchPayments(any())).thenReturn(List.of());
        long now = System.currentTimeMillis();
        Payment validated = paymentValidator.validatePaymentForCreate(
                paymentRequest(PaymentModeEnum.CHEQUE, now, "CHQ-1001", null));
        assertThat(validated.getInstrumentNumber()).isEqualTo("CHQ-1001");
    }

    @Test
    void validatePaymentForCreate_offlineNeftFutureDate_rejects() {
        when(paymentRepository.fetchPayments(any())).thenReturn(List.of());
        long future = System.currentTimeMillis() + 86_400_000L;
        assertThatThrownBy(() -> paymentValidator.validatePaymentForCreate(
                paymentRequest(PaymentModeEnum.OFFLINE_NEFT, future, "NEFT-1", null)))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateAndEnrichPaymentsForUpdate_remittedStatus_updatesPayment() {
        BillDetail billDetail = BillDetail.builder().id("bd-1").voucherHeader("VH-1").build();
        Bill billFromRequest = Bill.builder()
                .id("bill-1")
                .billDetails(new ArrayList<>(List.of(billDetail)))
                .build();
        PaymentDetail detailFromRequest = PaymentDetail.builder()
                .bill(billFromRequest)
                .build();
        Payment paymentUpdate = Payment.builder()
                .id("pay-1")
                .instrumentStatus(InstrumentStatusEnum.REMITTED)
                .paymentDetails(new ArrayList<>(List.of(detailFromRequest)))
                .build();

        Bill billFromDb = Bill.builder()
                .id("bill-1")
                .auditDetails(new AuditDetails())
                .billDetails(new ArrayList<>(List.of(BillDetail.builder().id("bd-1").build())))
                .build();
        PaymentDetail detailFromDb = PaymentDetail.builder()
                .billId("bill-1")
                .bill(billFromDb)
                .auditDetails(new AuditDetails())
                .build();
        Payment paymentFromDb = Payment.builder()
                .id("pay-1")
                .paymentMode(PaymentModeEnum.CASH)
                .instrumentStatus(InstrumentStatusEnum.APPROVED)
                .paymentStatus(PaymentStatusEnum.NEW)
                .auditDetails(new AuditDetails())
                .paymentDetails(new ArrayList<>(List.of(detailFromDb)))
                .build();

        when(paymentRepository.fetchPayments(any())).thenReturn(new ArrayList<>(List.of(paymentFromDb)));

        User user = new User();
        user.setId(1L);
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setUserInfo(user);

        List<Payment> updated = paymentValidator.validateAndEnrichPaymentsForUpdate(
                List.of(paymentUpdate), requestInfo);

        assertThat(updated.get(0).getInstrumentStatus()).isEqualTo(InstrumentStatusEnum.REMITTED);
        assertThat(updated.get(0).getPaymentStatus()).isEqualTo(PaymentStatusEnum.DEPOSITED);
    }

    @Test
    void validateAndEnrichPaymentsForUpdate_mergesBillFields() {
        BillDetail billDetail = BillDetail.builder().id("bd-1").build();
        Bill billFromRequest = Bill.builder()
                .id("bill-1")
                .paidBy("new-payer")
                .billDetails(new ArrayList<>(List.of(billDetail)))
                .build();
        PaymentDetail detailFromRequest = PaymentDetail.builder().bill(billFromRequest).build();

        Bill billFromDb = Bill.builder()
                .id("bill-1")
                .auditDetails(new AuditDetails())
                .billDetails(new ArrayList<>(List.of(BillDetail.builder().id("bd-1").build())))
                .build();
        PaymentDetail detailFromDb = PaymentDetail.builder()
                .billId("bill-1")
                .bill(billFromDb)
                .auditDetails(new AuditDetails())
                .build();

        Payment paymentFromDb = Payment.builder()
                .id("pay-1")
                .paymentMode(PaymentModeEnum.CASH)
                .instrumentStatus(InstrumentStatusEnum.APPROVED)
                .paymentStatus(PaymentStatusEnum.NEW)
                .auditDetails(new AuditDetails())
                .paymentDetails(new ArrayList<>(List.of(detailFromDb)))
                .build();

        Payment paymentUpdate = Payment.builder()
                .id("pay-1")
                .paymentDetails(new ArrayList<>(List.of(detailFromRequest)))
                .build();

        when(paymentRepository.fetchPayments(any())).thenReturn(new ArrayList<>(List.of(paymentFromDb)));

        User user = new User();
        user.setId(1L);
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setUserInfo(user);

        List<Payment> updated = paymentValidator.validateAndEnrichPaymentsForUpdate(
                List.of(paymentUpdate), requestInfo);

        assertThat(updated).hasSize(1);
        assertThat(updated.get(0).getPaymentDetails().get(0).getBill().getPaidBy()).isEqualTo("new-payer");
    }

    @Test
    void validatePaymentForCreate_partialPaymentNotAllowed_rejects() {
        when(paymentRepository.fetchPayments(any())).thenReturn(List.of());
        PaymentRequest request = paymentRequest(PaymentModeEnum.CASH, null, null, null);
        request.getPayment().getPaymentDetails().get(0).setTotalAmountPaid(BigDecimal.valueOf(50));
        request.getPayment().getPaymentDetails().get(0).getBill().setPartPaymentAllowed(false);
        assertThatThrownBy(() -> paymentValidator.validatePaymentForCreate(request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validatePaymentForCreate_chequeWithTransactionDate_validatesDateWindow() {
        when(paymentRepository.fetchPayments(any())).thenReturn(List.of());
        long txnDate = System.currentTimeMillis();
        PaymentRequest request = paymentRequest(PaymentModeEnum.CHEQUE, txnDate, "CHQ-2", null);
        request.getPayment().setTransactionDate(txnDate);
        Payment validated = paymentValidator.validatePaymentForCreate(request);
        assertThat(validated.getPaymentMode()).isEqualTo(PaymentModeEnum.CHEQUE);
    }

    @Test
    void validatePaymentForCreate_collectionModeNotAllowed_rejects() {
        when(paymentRepository.fetchPayments(any())).thenReturn(List.of());
        PaymentRequest request = paymentRequest(PaymentModeEnum.CASH, null, null, null);
        request.getPayment().getPaymentDetails().get(0).getBill()
                .setCollectionModesNotAllowed(new ArrayList<>(List.of("CASH")));
        assertThatThrownBy(() -> paymentValidator.validatePaymentForCreate(request))
                .isInstanceOf(CustomException.class);
    }

    private PaymentRequest paymentRequest(PaymentModeEnum mode, Long instrumentDate, String instrumentNumber,
            String transactionNumber) {
        BillDetail billDetail = BillDetail.builder()
                .id("bd-1")
                .expiryDate(System.currentTimeMillis() + 86_400_000L)
                .build();
        Bill bill = Bill.builder()
                .id("bill-1")
                .tenantId("pb")
                .totalAmount(BigDecimal.valueOf(100))
                .amountPaid(BigDecimal.ZERO)
                .isAdvanceAllowed(true)
                .partPaymentAllowed(true)
                .billDetails(new ArrayList<>(List.of(billDetail)))
                .build();

        PaymentDetail paymentDetail = PaymentDetail.builder()
                .billId("bill-1")
                .businessService("PT")
                .totalDue(BigDecimal.valueOf(100))
                .totalAmountPaid(BigDecimal.valueOf(100))
                .bill(bill)
                .build();

        Payment payment = Payment.builder()
                .tenantId("pb")
                .paymentMode(mode)
                .instrumentDate(instrumentDate)
                .instrumentNumber(instrumentNumber)
                .transactionNumber(transactionNumber != null ? transactionNumber : "TXN-1")
                .transactionDate(System.currentTimeMillis())
                .paymentDetails(List.of(paymentDetail))
                .build();

        User user = new User();
        user.setUuid("uuid-1");
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setUserInfo(user);

        return PaymentRequest.builder().payment(payment).requestInfo(requestInfo).build();
    }
}
