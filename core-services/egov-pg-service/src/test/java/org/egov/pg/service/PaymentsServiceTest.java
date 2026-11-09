package org.egov.pg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.pg.config.AppProperties;
import org.egov.pg.models.AuditDetails;
import org.egov.pg.models.CollectionPayment;
import org.egov.pg.models.CollectionPaymentResponse;
import org.egov.pg.models.TaxAndPayment;
import org.egov.pg.models.Transaction;
import org.egov.pg.models.enums.CollectionPaymentModeEnum;
import org.egov.pg.producer.Producer;
import org.egov.pg.repository.ServiceCallRepository;
import org.egov.pg.web.models.TransactionRequest;
import org.egov.tracer.model.CustomException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PaymentsServiceTest {

    @Mock
    private ServiceCallRepository repository;

    @Mock
    private AppProperties props;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private Producer producer;

    @InjectMocks
    private PaymentsService paymentsService;

    private TransactionRequest transactionRequest;
    private CollectionPayment registeredPayment;

    @Before
    public void setUp() {
        org.egov.pg.web.models.User pgUser = org.egov.pg.web.models.User.builder()
                .name("Test User")
                .mobileNumber("9999999999")
                .tenantId("pb")
                .build();
        TaxAndPayment taxAndPayment = TaxAndPayment.builder()
                .billId("bill-1")
                .amountPaid(new BigDecimal("100"))
                .build();
        AuditDetails auditDetails = AuditDetails.builder()
                .createdBy("user-uuid")
                .createdTime(1_700_000_000_000L)
                .lastModifiedBy("user-uuid")
                .lastModifiedTime(1_700_000_000_000L)
                .build();
        Transaction transaction = Transaction.builder()
                .tenantId("pb")
                .txnAmount("100")
                .billId("bill-1")
                .consumerCode("PT-001")
                .txnId("TXN-001")
                .user(pgUser)
                .taxAndPayments(Collections.singletonList(taxAndPayment))
                .auditDetails(auditDetails)
                .additionalDetails(JsonNodeFactory.instance.objectNode().put("key", "value"))
                .build();
        User requestUser = User.builder().uuid("req-user-uuid").build();
        RequestInfo requestInfo = RequestInfo.builder().userInfo(requestUser).build();
        transactionRequest = new TransactionRequest(requestInfo, transaction);

        registeredPayment = CollectionPayment.builder()
                .tenantId("pb")
                .totalAmountPaid(new BigDecimal("100"))
                .build();

        when(props.getCollectionServiceHost()).thenReturn("http://collection/");
        when(props.getPaymentCreatePath()).thenReturn("payment/_create");
        when(props.getPaymentValidatePath()).thenReturn("payment/_validate");
        when(props.getPaymentRefundTopic()).thenReturn("egov.collection.payment.refund");
    }

    @Test
    public void getPaymentFromTransactionBuildsCollectionPayment() {
        CollectionPayment payment = paymentsService.getPaymentFromTransaction(transactionRequest);

        assertEquals("pb", payment.getTenantId());
        assertEquals(new BigDecimal("100"), payment.getTotalAmountPaid());
        assertEquals(CollectionPaymentModeEnum.ONLINE, payment.getPaymentMode());
        assertEquals("Test User", payment.getPaidBy());
        assertEquals("9999999999", payment.getMobileNumber());
        assertEquals(1, payment.getPaymentDetails().size());
        assertEquals("bill-1", payment.getPaymentDetails().get(0).getBillId());
    }

    @Test
    public void registerPaymentSuccess() {
        CollectionPaymentResponse paymentResponse = CollectionPaymentResponse.builder()
                .payments(Collections.singletonList(registeredPayment))
                .build();
        when(repository.fetchResult(any(String.class), any())).thenReturn(Optional.of(new Object()));
        when(mapper.convertValue(any(), eq(CollectionPaymentResponse.class))).thenReturn(paymentResponse);

        CollectionPayment result = paymentsService.registerPayment(transactionRequest);

        assertEquals(registeredPayment, result);
        verify(repository).fetchResult(eq("http://collection/payment/_create"), any());
    }

    @Test(expected = CustomException.class)
    public void registerPaymentEmptyResponse() {
        when(repository.fetchResult(any(String.class), any())).thenReturn(Optional.empty());

        try {
            paymentsService.registerPayment(transactionRequest);
        } catch (CustomException e) {
            assertEquals("PAYMENT_REGISTRATION_FAILED", e.getCode());
            throw e;
        }
    }

    @Test(expected = CustomException.class)
    public void registerPaymentEmptyPaymentsList() {
        CollectionPaymentResponse paymentResponse = CollectionPaymentResponse.builder()
                .payments(Collections.emptyList())
                .build();
        when(repository.fetchResult(any(String.class), any())).thenReturn(Optional.of(new Object()));
        when(mapper.convertValue(any(), eq(CollectionPaymentResponse.class))).thenReturn(paymentResponse);

        try {
            paymentsService.registerPayment(transactionRequest);
        } catch (CustomException e) {
            assertEquals("RESPONSE_PARSE_ERROR", e.getCode());
            throw e;
        }
    }

    @Test(expected = CustomException.class)
    public void registerPaymentParseError() {
        when(repository.fetchResult(any(String.class), any())).thenReturn(Optional.of(new Object()));
        when(mapper.convertValue(any(), eq(CollectionPaymentResponse.class)))
                .thenThrow(new IllegalArgumentException("bad json"));

        try {
            paymentsService.registerPayment(transactionRequest);
        } catch (CustomException e) {
            assertEquals("RESPONSE_PARSE_ERROR", e.getCode());
            throw e;
        }
    }

    @Test
    public void validatePaymentSuccess() {
        CollectionPaymentResponse paymentResponse = CollectionPaymentResponse.builder()
                .payments(Collections.singletonList(registeredPayment))
                .build();
        when(repository.fetchResult(any(String.class), any())).thenReturn(Optional.of(new Object()));
        when(mapper.convertValue(any(), eq(CollectionPaymentResponse.class))).thenReturn(paymentResponse);

        CollectionPayment result = paymentsService.validatePayment(transactionRequest);

        assertEquals(registeredPayment, result);
        verify(repository).fetchResult(eq("http://collection/payment/_validate"), any());
    }

    @Test(expected = CustomException.class)
    public void validatePaymentEmptyResponse() {
        when(repository.fetchResult(any(String.class), any())).thenReturn(Optional.empty());

        try {
            paymentsService.validatePayment(transactionRequest);
        } catch (CustomException e) {
            assertEquals("PAYMENT_VALIDATION_FAILED", e.getCode());
            throw e;
        }
    }

    @Test(expected = CustomException.class)
    public void validatePaymentEmptyPaymentsList() {
        CollectionPaymentResponse paymentResponse = CollectionPaymentResponse.builder()
                .payments(Collections.emptyList())
                .build();
        when(repository.fetchResult(any(String.class), any())).thenReturn(Optional.of(new Object()));
        when(mapper.convertValue(any(), eq(CollectionPaymentResponse.class))).thenReturn(paymentResponse);

        try {
            paymentsService.validatePayment(transactionRequest);
        } catch (CustomException e) {
            assertEquals("RESPONSE_PARSE_ERROR", e.getCode());
            throw e;
        }
    }

    @Test(expected = CustomException.class)
    public void validatePaymentParseError() {
        when(repository.fetchResult(any(String.class), any())).thenReturn(Optional.of(new Object()));
        when(mapper.convertValue(any(), eq(CollectionPaymentResponse.class)))
                .thenThrow(new RuntimeException("mapping failed"));

        try {
            paymentsService.validatePayment(transactionRequest);
        } catch (CustomException e) {
            assertEquals("RESPONSE_PARSE_ERROR", e.getCode());
            throw e;
        }
    }

    @Test
    public void refundTransactionWithExistingAudit() {
        paymentsService.refundTransaction(transactionRequest);

        verify(producer).push(eq("egov.collection.payment.refund"), any());
    }

    @Test
    public void refundTransactionNullAuditUsesRequestUser() {
        transactionRequest.getTransaction().setAuditDetails(null);

        paymentsService.refundTransaction(transactionRequest);

        verify(producer).push(eq("egov.collection.payment.refund"), any());
    }

    @Test
    public void refundTransactionNullAuditAndNullRequestUser() {
        transactionRequest.getTransaction().setAuditDetails(null);
        transactionRequest.setRequestInfo(RequestInfo.builder().userInfo(null).build());

        paymentsService.refundTransaction(transactionRequest);

        verify(producer).push(eq("egov.collection.payment.refund"), any());
    }

}
