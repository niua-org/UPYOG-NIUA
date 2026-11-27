package org.egov.collection.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.egov.collection.config.ApplicationProperties;
import org.egov.collection.model.AuditDetails;
import org.egov.collection.model.Payment;
import org.egov.collection.model.PaymentDetail;
import org.egov.collection.model.enums.InstrumentStatusEnum;
import org.egov.collection.model.enums.PaymentModeEnum;
import org.egov.collection.model.enums.PaymentStatusEnum;
import org.egov.collection.producer.CollectionProducer;
import org.egov.collection.repository.PaymentRepository;
import org.egov.collection.util.PaymentWorkflowValidator;
import org.egov.collection.web.contract.Bill;
import org.egov.collection.web.contract.PaymentWorkflow;
import org.egov.collection.web.contract.PaymentWorkflowRequest;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentWorkflowServiceBehaviorTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentWorkflowValidator paymentWorkflowValidator;
    @Mock
    private CollectionProducer collectionProducer;

    private ApplicationProperties applicationProperties;
    private PaymentWorkflowService paymentWorkflowService;

    @BeforeEach
    void setUp() {
        applicationProperties = new ApplicationProperties();
        applicationProperties.setReceiptsSearchDefaultLimit(100);
        applicationProperties.setCancelPaymentTopicName("cancel-topic");
        paymentWorkflowService = new PaymentWorkflowService(paymentRepository, paymentWorkflowValidator,
                collectionProducer, applicationProperties);
    }

    @Test
    void performWorkflow_cancel_updatesPaymentAndPublishes() {
        Payment payment = samplePayment("pay-1", PaymentModeEnum.CASH, InstrumentStatusEnum.APPROVED);
        when(paymentRepository.fetchPayments(any())).thenReturn(new ArrayList<>(List.of(payment)));
        when(paymentWorkflowValidator.validateForCancel(any(), any())).thenReturn(new ArrayList<>(List.of(payment)));
        doNothing().when(paymentRepository).updateStatus(any());
        doNothing().when(paymentRepository).updateFileStoreId(any());
        doNothing().when(collectionProducer).producer(any(), any());

        PaymentWorkflow workflow = new PaymentWorkflow();
        workflow.setAction(PaymentWorkflow.PaymentAction.CANCEL);
        workflow.setPaymentId("pay-1");
        workflow.setTenantId("pb");
        workflow.setReason("duplicate");
        workflow.setAdditionalDetails(JsonNodeFactory.instance.objectNode());

        PaymentWorkflowRequest request = new PaymentWorkflowRequest();
        request.setRequestInfo(requestInfoWithUser());
        request.setPaymentWorkflows(List.of(workflow));

        @SuppressWarnings("unchecked")
        List<Payment> result = (List<Payment>) paymentWorkflowService.performWorkflow(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPaymentStatus()).isEqualTo(PaymentStatusEnum.CANCELLED);
        assertThat(result.get(0).getPaymentDetails().get(0).getBill().getIsCancelled()).isTrue();
        verify(collectionProducer).producer(any(), any());
    }

    @Test
    void performWorkflow_remit_marksDeposited() {
        Payment payment = samplePayment("pay-2", PaymentModeEnum.CASH, InstrumentStatusEnum.APPROVED);
        when(paymentRepository.fetchPayments(any())).thenReturn(new ArrayList<>(List.of(payment)));
        when(paymentWorkflowValidator.validateForRemit(any(), any())).thenReturn(new ArrayList<>(List.of(payment)));
        doNothing().when(paymentRepository).updateStatus(any());
        doNothing().when(paymentRepository).updateFileStoreId(any());
        doNothing().when(collectionProducer).producer(any(), any());

        PaymentWorkflow workflow = new PaymentWorkflow();
        workflow.setAction(PaymentWorkflow.PaymentAction.REMIT);
        workflow.setPaymentId("pay-2");
        workflow.setTenantId("pb");
        workflow.setReason("remit");
        workflow.setAdditionalDetails(JsonNodeFactory.instance.objectNode());

        PaymentWorkflowRequest request = new PaymentWorkflowRequest();
        request.setRequestInfo(requestInfoWithUser());
        request.setPaymentWorkflows(List.of(workflow));

        @SuppressWarnings("unchecked")
        List<Payment> result = (List<Payment>) paymentWorkflowService.performWorkflow(request);

        assertThat(result.get(0).getPaymentStatus()).isEqualTo(PaymentStatusEnum.DEPOSITED);
        assertThat(result.get(0).getInstrumentStatus()).isEqualTo(InstrumentStatusEnum.REMITTED);
    }

    @Test
    void performWorkflow_dishonour_marksDishonoured() {
        Payment payment = samplePayment("pay-3", PaymentModeEnum.CHEQUE, InstrumentStatusEnum.APPROVED);
        when(paymentRepository.fetchPayments(any())).thenReturn(new ArrayList<>(List.of(payment)));
        when(paymentWorkflowValidator.validateForCancel(any(), any())).thenReturn(new ArrayList<>(List.of(payment)));
        doNothing().when(paymentRepository).updateStatus(any());
        doNothing().when(paymentRepository).updateFileStoreId(any());
        doNothing().when(collectionProducer).producer(any(), any());

        PaymentWorkflow workflow = new PaymentWorkflow();
        workflow.setAction(PaymentWorkflow.PaymentAction.DISHONOUR);
        workflow.setPaymentId("pay-3");
        workflow.setTenantId("pb");
        workflow.setReason("bounce");
        workflow.setAdditionalDetails(JsonNodeFactory.instance.objectNode());

        PaymentWorkflowRequest request = new PaymentWorkflowRequest();
        request.setRequestInfo(requestInfoWithUser());
        request.setPaymentWorkflows(List.of(workflow));

        @SuppressWarnings("unchecked")
        List<Payment> result = (List<Payment>) paymentWorkflowService.performWorkflow(request);

        assertThat(result.get(0).getPaymentStatus()).isEqualTo(PaymentStatusEnum.DISHONOURED);
    }

    @Test
    void performWorkflow_crossTenant_rejected() {
        PaymentWorkflow w1 = new PaymentWorkflow();
        w1.setAction(PaymentWorkflow.PaymentAction.CANCEL);
        w1.setPaymentId("pay-1");
        w1.setTenantId("pb");

        PaymentWorkflow w2 = new PaymentWorkflow();
        w2.setAction(PaymentWorkflow.PaymentAction.CANCEL);
        w2.setPaymentId("pay-2");
        w2.setTenantId("pb.amritsar");

        PaymentWorkflowRequest request = new PaymentWorkflowRequest();
        request.setRequestInfo(requestInfoWithUser());
        request.setPaymentWorkflows(List.of(w1, w2));

        assertThatThrownBy(() -> paymentWorkflowService.performWorkflow(request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void performWorkflow_mixedActions_rejected() {
        PaymentWorkflow cancel = new PaymentWorkflow();
        cancel.setAction(PaymentWorkflow.PaymentAction.CANCEL);
        cancel.setPaymentId("pay-1");
        cancel.setTenantId("pb");

        PaymentWorkflow remit = new PaymentWorkflow();
        remit.setAction(PaymentWorkflow.PaymentAction.REMIT);
        remit.setPaymentId("pay-1");
        remit.setTenantId("pb");

        PaymentWorkflowRequest request = new PaymentWorkflowRequest();
        request.setRequestInfo(requestInfoWithUser());
        request.setPaymentWorkflows(List.of(cancel, remit));

        when(paymentRepository.fetchPayments(any())).thenReturn(new ArrayList<>(List.of(samplePayment("pay-1",
                PaymentModeEnum.CASH, InstrumentStatusEnum.APPROVED))));

        assertThatThrownBy(() -> paymentWorkflowService.performWorkflow(request))
                .isInstanceOf(CustomException.class);
    }

    private RequestInfo requestInfoWithUser() {
        User user = new User();
        user.setUuid("user-uuid");
        user.setId(1L);
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setUserInfo(user);
        return requestInfo;
    }

    private Payment samplePayment(String id, PaymentModeEnum mode, InstrumentStatusEnum instrumentStatus) {
        Bill bill = Bill.builder()
                .id("bill-1")
                .tenantId("pb")
                .consumerCode("CC-1")
                .totalAmount(BigDecimal.valueOf(100))
                .status(Bill.StatusEnum.ACTIVE)
                .isCancelled(false)
                .auditDetails(new AuditDetails())
                .billDetails(new ArrayList<>(List.of()))
                .build();

        PaymentDetail paymentDetail = PaymentDetail.builder()
                .id("pd-1")
                .tenantId("pb")
                .bill(bill)
                .auditDetails(new AuditDetails())
                .build();

        return Payment.builder()
                .id(id)
                .tenantId("pb")
                .paymentMode(mode)
                .instrumentStatus(instrumentStatus)
                .paymentStatus(PaymentStatusEnum.NEW)
                .transactionDate(1L)
                .totalDue(BigDecimal.ZERO)
                .totalAmountPaid(BigDecimal.valueOf(100))
                .auditDetails(new AuditDetails())
                .paymentDetails(new ArrayList<>(List.of(paymentDetail)))
                .build();
    }
}
