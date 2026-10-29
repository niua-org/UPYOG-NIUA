package org.egov.collection.util;

import org.egov.collection.model.Payment;
import org.egov.collection.model.PaymentDetail;
import org.egov.collection.model.PaymentRequest;
import org.egov.collection.model.enums.PaymentModeEnum;
import org.egov.collection.repository.BillingServiceRepository;
import org.egov.collection.repository.IdGenRepository;
import org.egov.collection.service.MDMSService;
import org.egov.collection.web.contract.Bill;
import org.egov.collection.web.contract.BillDetail;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEnricherBehaviorTest {

    @Mock
    private BillingServiceRepository billingServiceRepository;
    @Mock
    private IdGenRepository idGenRepository;
    @Mock
    private MDMSService mdmsService;

    private PaymentEnricher paymentEnricher;

    @BeforeEach
    void setUp() {
        paymentEnricher = new PaymentEnricher();
        ReflectionTestUtils.setField(paymentEnricher, "billingRepository", billingServiceRepository);
        ReflectionTestUtils.setField(paymentEnricher, "idGenRepository", idGenRepository);
        ReflectionTestUtils.setField(paymentEnricher, "mdmsService", mdmsService);
    }

    @Test
    void enrichPaymentPreValidate_assignsBillAndTotals() {
        Map<String, Object> mdms = Map.of("MdmsRes", Map.of("BillingService", Map.of("BusinessService",
                List.of(Map.of("code", "PT", "collectionModesNotAllowed", List.of(), "isAdvanceAllowed", true,
                        "partPaymentAllowed", true)))));
        when(mdmsService.mDMSCall(any(), any())).thenReturn(mdms);

        BillDetail billDetail = BillDetail.builder().id("bd-1").expiryDate(System.currentTimeMillis() + 1000).build();
        Bill bill = Bill.builder()
                .id("bill-1")
                .businessService("PT")
                .totalAmount(BigDecimal.valueOf(100))
                .billDetails(new ArrayList<>(List.of(billDetail)))
                .build();
        when(billingServiceRepository.fetchBill(any(), any(), anyList())).thenReturn(new ArrayList<>(List.of(bill)));

        PaymentDetail paymentDetail = PaymentDetail.builder().billId("bill-1").totalAmountPaid(BigDecimal.valueOf(100)).build();
        Payment payment = Payment.builder()
                .tenantId("pb")
                .paidBy("citizen")
                .paymentDetails(new ArrayList<>(List.of(paymentDetail)))
                .build();

        User user = new User();
        user.setId(1L);
        user.setUuid("uuid-1");
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setUserInfo(user);
        PaymentRequest paymentRequest = PaymentRequest.builder().payment(payment).requestInfo(requestInfo).build();

        paymentEnricher.enrichPaymentPreValidate(paymentRequest);

        assertThat(payment.getId()).isNotBlank();
        assertThat(payment.getTotalDue()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(paymentDetail.getBill()).isNotNull();
    }

    @Test
    void enrichAdvanceTaxHead_assignsAdvanceAccountIds() {
        org.egov.collection.web.contract.BillAccountDetail advance = org.egov.collection.web.contract.BillAccountDetail.builder()
                .purpose(org.egov.collection.model.enums.Purpose.ADVANCE)
                .amount(BigDecimal.TEN)
                .build();
        BillDetail billDetail = BillDetail.builder()
                .id("bd-1")
                .billAccountDetails(new ArrayList<>(List.of(advance)))
                .build();
        Bill bill = Bill.builder()
                .id("bill-1")
                .tenantId("pb")
                .billDetails(new ArrayList<>(List.of(billDetail)))
                .build();

        paymentEnricher.enrichAdvanceTaxHead(new ArrayList<>(List.of(bill)));

        assertThat(advance.getId()).isNotBlank();
        assertThat(advance.getDemandDetailId()).isEqualTo("advance_tax");
    }

    @Test
    void enrichPaymentPostValidate_onlinePayment_marksDepositedAndRemitted() {
        when(idGenRepository.generateReceiptNumber(any(), any(), any())).thenReturn("RCPT-ONL");

        Bill bill = Bill.builder().id("bill-1").totalAmount(BigDecimal.TEN).build();
        PaymentDetail paymentDetail = PaymentDetail.builder()
                .businessService("PT")
                .tenantId("pb")
                .totalAmountPaid(BigDecimal.TEN)
                .bill(bill)
                .build();
        Payment payment = Payment.builder()
                .tenantId("pb")
                .paymentMode(PaymentModeEnum.ONLINE)
                .transactionNumber("TXN-ONL")
                .instrumentNumber("INST-ONL")
                .paymentDetails(new ArrayList<>(List.of(paymentDetail)))
                .build();
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .payment(payment)
                .requestInfo(new RequestInfo())
                .build();

        paymentEnricher.enrichPaymentPostValidate(paymentRequest);

        assertThat(payment.getPaymentStatus()).isEqualTo(org.egov.collection.model.enums.PaymentStatusEnum.DEPOSITED);
        assertThat(payment.getInstrumentStatus()).isEqualTo(org.egov.collection.model.enums.InstrumentStatusEnum.REMITTED);
    }

    @Test
    void enrichPaymentPostValidate_cashPayment_generatesReceiptNumber() {
        when(idGenRepository.generateReceiptNumber(any(), any(), any())).thenReturn("RCPT-001");
        when(idGenRepository.generateTransactionNumber(any(), any())).thenReturn("TXN-001");

        Bill bill = Bill.builder().id("bill-1").totalAmount(BigDecimal.TEN).build();
        PaymentDetail paymentDetail = PaymentDetail.builder()
                .businessService("PT")
                .tenantId("pb")
                .totalAmountPaid(BigDecimal.TEN)
                .bill(bill)
                .build();
        Payment payment = Payment.builder()
                .tenantId("pb")
                .paymentMode(PaymentModeEnum.CASH)
                .paymentDetails(new ArrayList<>(List.of(paymentDetail)))
                .build();
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .payment(payment)
                .requestInfo(new RequestInfo())
                .build();

        paymentEnricher.enrichPaymentPostValidate(paymentRequest);

        assertThat(paymentDetail.getReceiptNumber()).isEqualTo("RCPT-001");
        assertThat(payment.getTransactionNumber()).isEqualTo("TXN-001");
    }
}
