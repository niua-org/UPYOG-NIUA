package org.egov.pg.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.pg.config.AppProperties;
import org.egov.pg.models.CollectionPayment;
import org.egov.pg.models.PaymentRefund;
import org.egov.pg.models.PaymentRequest;
import org.egov.pg.models.Refund;
import org.egov.pg.models.Refund.RefundStatusEnum;
import org.egov.pg.models.RefundRequest;
import org.egov.pg.models.Transaction;
import org.egov.pg.producer.Producer;
import org.egov.pg.repository.RefundRepository;
import org.egov.pg.repository.TransactionRepository;
import org.egov.pg.validator.RefundValidator;
import org.egov.pg.web.models.RefundCriteria;
import org.egov.pg.web.models.TransactionCriteria;
import org.egov.tracer.model.CustomException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.TransientDataAccessResourceException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RefundServiceTest {

    @Mock
    private RefundValidator refundValidator;
    @Mock
    private EnrichmentService enrichmentService;
    @Mock
    private RefundRepository refundRepository;
    @Mock
    private Producer producer;
    @Mock
    private AppProperties appProperties;
    @Mock
    private GatewayService gatewayService;
    @Mock
    private PaymentsService paymentsService;
    @Mock
    private TransactionRepository transactionRepository;

    private RefundService refundService;
    private RequestInfo requestInfo;

    @Before
    public void setUp() {
        refundService = new RefundService(refundValidator, enrichmentService, refundRepository, producer,
                appProperties, gatewayService, paymentsService, transactionRepository);
        requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("user-1").build())
                .build();
        when(appProperties.getSaveRefundTxnsTopic()).thenReturn("save-refund");
        when(appProperties.getUpdateRefundTxnsTopic()).thenReturn("update-refund");
    }

    @Test
    public void initiateRefundSuccessTriggersPaymentRefund() {
        Refund refund = Refund.builder()
                .originalTxnId("TXN-1")
                .status(RefundStatusEnum.INITIATED)
                .build();
        RefundRequest refundRequest = new RefundRequest(requestInfo, refund);
        Refund gatewayResponse = Refund.builder()
                .originalTxnId("TXN-1")
                .status(RefundStatusEnum.SUCCESS)
                .build();
        when(gatewayService.initiateRefund(refund)).thenReturn(gatewayResponse);
        when(transactionRepository.fetchTransactions(any(TransactionCriteria.class)))
                .thenReturn(Collections.singletonList(Transaction.builder().txnId("TXN-1").build()));

        Refund result = refundService.initiateRefund(refundRequest);

        assertEquals(RefundStatusEnum.SUCCESS, result.getStatus());
        verify(paymentsService).refundTransaction(any());
        verify(producer).push(eq("save-refund"), any(RefundRequest.class));
        verify(producer).push(eq("update-refund"), any(RefundRequest.class));
    }

    @Test
    public void initiateRefundNonSuccessSkipsPaymentRefund() {
        Refund refund = Refund.builder().status(RefundStatusEnum.INITIATED).build();
        RefundRequest refundRequest = new RefundRequest(requestInfo, refund);
        when(gatewayService.initiateRefund(refund)).thenReturn(Refund.builder().status(RefundStatusEnum.FAILURE).build());

        refundService.initiateRefund(refundRequest);

        verify(paymentsService, never()).refundTransaction(any());
    }

    @Test
    public void getRefundTransactionReturnsList() {
        RefundCriteria criteria = RefundCriteria.builder().refundId("RF-1").build();
        when(refundRepository.fetchRefundTransactions(criteria))
                .thenReturn(Collections.singletonList(Refund.builder().refundId("RF-1").build()));

        assertEquals(1, refundService.getRefundTransaction(criteria).size());
    }

    @Test(expected = CustomException.class)
    public void getRefundTransactionDbError() {
        when(refundRepository.fetchRefundTransactions(any())).thenThrow(new TransientDataAccessResourceException("db"));
        refundService.getRefundTransaction(RefundCriteria.builder().build());
    }

    @Test
    public void updateRefundTransactionSkipGateway() {
        Refund current = Refund.builder().refundId("RF-2").refundAmount(BigDecimal.ZERO).build();
        Map<String, String> params = Collections.singletonMap("refundId", "RF-2");
        when(refundValidator.validateUpdateRefundTransaction(params)).thenReturn(current);
        when(refundValidator.skipGateway(current)).thenReturn(true);

        List<Refund> updated = refundService.updateRefundTransaction(requestInfo, params);

        assertSame(current, updated.get(0));
        verify(gatewayService, never()).getRefundLiveStatus(any());
    }

    @Test(expected = CustomException.class)
    public void processRefundMissingTransactionNumber() {
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .requestInfo(requestInfo)
                .payment(CollectionPayment.builder().transactionNumber("").build())
                .build();
        refundService.processRefund(paymentRequest);
    }

    @Test
    public void processRefundBuildsPaymentRefund() {
        CollectionPayment payment = CollectionPayment.builder()
                .tenantId("pb")
                .transactionNumber("TXN-99")
                .build();
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .requestInfo(requestInfo)
                .payment(payment)
                .build();
        Transaction txn = Transaction.builder().txnId("TXN-99").txnAmount("100").build();
        Refund initiated = Refund.builder().refundId("RF-99").status(RefundStatusEnum.INITIATED).build();
        RefundRequest refundRequest = new RefundRequest(requestInfo, initiated);

        when(transactionRepository.fetchTransactions(any(TransactionCriteria.class)))
                .thenReturn(Collections.singletonList(txn));
        when(enrichmentService.enrichRefundRequest(any(), eq(requestInfo))).thenReturn(refundRequest);
        when(gatewayService.initiateRefund(initiated)).thenReturn(initiated);
        doAnswer(invocation -> {
            PaymentRefund target = invocation.getArgument(1);
            target.setRefundId("RF-99");
            return null;
        }).when(enrichmentService).paymentRefundResponse(eq(initiated), any(PaymentRefund.class), eq(payment));

        PaymentRefund paymentRefund = refundService.processRefund(paymentRequest);

        verify(enrichmentService).paymentRefundResponse(eq(initiated), any(PaymentRefund.class), eq(payment));
        assertEquals("RF-99", paymentRefund.getRefundId());
    }
}
