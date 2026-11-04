package org.egov.collection.util;

import org.egov.collection.model.AuditDetails;
import org.egov.collection.model.Payment;
import org.egov.collection.model.PaymentDetail;
import org.egov.collection.model.enums.InstrumentStatusEnum;
import org.egov.collection.model.enums.PaymentModeEnum;
import org.egov.collection.model.enums.PaymentStatusEnum;
import org.egov.collection.web.contract.Bill;
import org.egov.collection.web.contract.PaymentWorkflow;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentWorkflowValidatorBehaviorTest {

    private final PaymentWorkflowValidator validator = new PaymentWorkflowValidator();

    @Test
    void validateForRemit_returnsMatchingPayment() {
        Payment payment = samplePayment("pay-1", 100L);
        PaymentWorkflow workflow = new PaymentWorkflow();
        workflow.setPaymentId("pay-1");

        List<Payment> result = validator.validateForRemit(List.of(workflow), new ArrayList<>(List.of(payment)));

        assertThat(result).extracting(Payment::getId).containsExactly("pay-1");
    }

    @Test
    void validateForRemit_missingPayment_throws() {
        PaymentWorkflow workflow = new PaymentWorkflow();
        workflow.setPaymentId("missing");

        assertThatThrownBy(() -> validator.validateForRemit(List.of(workflow), new ArrayList<>()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateForCancel_latestPayment_succeeds() {
        Payment latest = samplePayment("pay-latest", 200L);
        PaymentWorkflow workflow = new PaymentWorkflow();
        workflow.setPaymentId("pay-latest");

        List<Payment> result = validator.validateForCancel(List.of(workflow), new ArrayList<>(List.of(latest)));

        assertThat(result).hasSize(1);
    }

    @Test
    void validateForRefund_onlinePayment_succeeds() {
        Payment payment = samplePayment("pay-1", 100L);
        payment.setPaymentMode(PaymentModeEnum.ONLINE);
        PaymentWorkflow workflow = new PaymentWorkflow();
        workflow.setPaymentId("pay-1");

        validator.validateForRefund(List.of(workflow), payment);
    }

    @Test
    void validateForRefund_nullPayment_throws() {
        assertThatThrownBy(() -> validator.validateForRefund(List.of(), null))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateForRefund_missingWorkflow_throws() {
        Payment payment = samplePayment("pay-1", 100L);
        payment.setPaymentMode(PaymentModeEnum.ONLINE);
        assertThatThrownBy(() -> validator.validateForRefund(List.of(), payment))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateForRefund_cashPayment_throws() {
        Payment payment = samplePayment("pay-1", 100L);
        PaymentWorkflow workflow = new PaymentWorkflow();
        workflow.setPaymentId("pay-1");

        assertThatThrownBy(() -> validator.validateForRefund(List.of(workflow), payment))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateForRefund_detailAmountExceedsTotal_throws() {
        Payment payment = samplePayment("pay-1", 100L);
        payment.setPaymentMode(PaymentModeEnum.ONLINE);
        payment.setTotalAmountPaid(BigDecimal.TEN);
        payment.getPaymentDetails().get(0).setTotalAmountPaid(BigDecimal.valueOf(20));
        PaymentWorkflow workflow = new PaymentWorkflow();
        workflow.setPaymentId("pay-1");

        assertThatThrownBy(() -> validator.validateForRefund(List.of(workflow), payment))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateForCancel_notLatestPayment_throws() {
        Payment older = samplePayment("pay-old", 100L);
        Payment latest = samplePayment("pay-latest", 200L);
        PaymentWorkflow workflow = new PaymentWorkflow();
        workflow.setPaymentId("pay-old");

        assertThatThrownBy(() -> validator.validateForCancel(List.of(workflow),
                new ArrayList<>(List.of(older, latest))))
                .isInstanceOf(CustomException.class);
    }

    private Payment samplePayment(String id, long transactionDate) {
        Bill bill = Bill.builder()
                .id("bill-1")
                .consumerCode("CC-1")
                .auditDetails(new AuditDetails())
                .build();
        PaymentDetail detail = PaymentDetail.builder()
                .bill(bill)
                .auditDetails(new AuditDetails())
                .build();
        return Payment.builder()
                .id(id)
                .transactionDate(transactionDate)
                .paymentMode(PaymentModeEnum.CASH)
                .instrumentStatus(InstrumentStatusEnum.APPROVED)
                .paymentStatus(PaymentStatusEnum.NEW)
                .totalDue(BigDecimal.ZERO)
                .totalAmountPaid(BigDecimal.TEN)
                .auditDetails(new AuditDetails())
                .paymentDetails(new ArrayList<>(List.of(detail)))
                .build();
    }
}
