package org.egov.pg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.pg.constants.PgConstants;
import org.egov.pg.models.AuditDetails;
import org.egov.pg.models.BankAccount;
import org.egov.pg.models.CollectionPayment;
import org.egov.pg.models.PaymentRefund;
import org.egov.pg.models.Refund;
import org.egov.pg.models.Refund.RefundStatusEnum;
import org.egov.pg.models.RefundRequest;
import org.egov.pg.models.TaxAndPayment;
import org.egov.pg.models.Transaction;
import org.egov.pg.repository.BankAccountRepository;
import org.egov.pg.repository.TransactionRepository;
import org.egov.pg.service.gateways.nttdata.RefundTransaction;
import org.egov.pg.web.models.TransactionCriteria;
import org.egov.pg.web.models.TransactionRequest;
import org.egov.tracer.model.CustomException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EnrichmentServiceTest {

    @Mock
    private IdGenService idGenService;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private UserService userService;

    @Mock
    private TransactionRepository transactionRepository;

    private ObjectMapper objectMapper;
    private EnrichmentService enrichmentService;

    private RequestInfo requestInfo;
    private Transaction transaction;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        enrichmentService = new EnrichmentService(
                idGenService, bankAccountRepository, objectMapper, userService, transactionRepository);

        User userInfo = User.builder()
                .uuid("user-uuid")
                .userName("citizen")
                .name("Citizen")
                .tenantId("pb")
                .mobileNumber("9999999999")
                .build();
        requestInfo = RequestInfo.builder().userInfo(userInfo).build();

        TaxAndPayment taxAndPayment = TaxAndPayment.builder()
                .amountPaid(new BigDecimal("100"))
                .taxAmount(new BigDecimal("100"))
                .build();
        transaction = Transaction.builder()
                .tenantId("pb")
                .txnAmount("100")
                .billId("bill-1")
                .consumerCode("PT-001")
                .callbackUrl("https://example.com/callback")
                .taxAndPayments(Collections.singletonList(taxAndPayment))
                .build();
    }

    @Test
    public void enrichCreateTransactionSetsIdsAndCallback() {
        org.egov.pg.web.models.User pgUser = org.egov.pg.web.models.User.builder()
                .mobileNumber("8888888888")
                .build();
        TransactionRequest request = new TransactionRequest(requestInfo, transaction);

        BankAccount bankAccount = new BankAccount();
        bankAccount.setAccountNumber("ACC123");
        when(bankAccountRepository.getBankAccountsById(requestInfo, "pb")).thenReturn(bankAccount);
        when(idGenService.generateTxnId(request)).thenReturn("TXN-GEN-001");
        when(userService.createOrSearchUser(request)).thenReturn(pgUser);

        enrichmentService.enrichCreateTransaction(request);

        assertEquals("TXN-GEN-001", transaction.getTxnId());
        assertEquals(Transaction.TxnStatusEnum.PENDING, transaction.getTxnStatus());
        assertEquals(PgConstants.TXN_INITIATED, transaction.getTxnStatusMsg());
        assertEquals(pgUser, transaction.getUser());
        assertTrue(transaction.getCallbackUrl().contains(PgConstants.PG_TXN_IN_LABEL));
        assertTrue(transaction.getCallbackUrl().contains("TXN-GEN-001"));
        assertNotNull(transaction.getAuditDetails());
        assertNotNull(transaction.getAdditionalDetails());
    }

    @Test
    public void enrichCreateTransactionMergesExistingAdditionalDetails() {
        Map<String, Object> existing = new HashMap<>();
        existing.put("key", "value");
        transaction.setAdditionalDetails(existing);
        TransactionRequest request = new TransactionRequest(requestInfo, transaction);

        BankAccount bankAccount = new BankAccount();
        bankAccount.setAccountNumber("ACC123");
        when(bankAccountRepository.getBankAccountsById(any(), any())).thenReturn(bankAccount);
        when(idGenService.generateTxnId(any())).thenReturn("TXN-002");
        when(userService.createOrSearchUser(any())).thenReturn(org.egov.pg.web.models.User.builder().build());

        enrichmentService.enrichCreateTransaction(request);

        assertNotNull(transaction.getAdditionalDetails());
    }

    @Test
    public void enrichUpdateTransactionCopiesFieldsAndAudit() {
        AuditDetails created = AuditDetails.builder().createdBy("creator").createdTime(100L).build();
        Transaction current = Transaction.builder()
                .txnId("TXN-1")
                .gateway("PAYTM")
                .billId("bill")
                .productInfo("info")
                .tenantId("pb")
                .consumerCode("PT-1")
                .txnStatusMsg("pending")
                .auditDetails(created)
                .build();
        Transaction updated = Transaction.builder().txnAmount("100").build();
        TransactionRequest request = new TransactionRequest(requestInfo, current);

        enrichmentService.enrichUpdateTransaction(request, updated);

        assertEquals("TXN-1", updated.getTxnId());
        assertEquals("PAYTM", updated.getGateway());
        assertEquals("creator", updated.getAuditDetails().getCreatedBy());
        assertEquals("user-uuid", updated.getAuditDetails().getLastModifiedBy());
        assertNotNull(updated.getAuditDetails().getLastModifiedTime());
    }

    @Test
    public void enrichupdateRefundTransactionSetsAtomTxnId() {
        Refund refund = Refund.builder().originalTxnId("TXN-REF").build();
        Transaction stored = Transaction.builder().atomTxnId("ATOM-1").consumerCode("CC-1").build();
        when(transactionRepository.fetchTransactions(any(TransactionCriteria.class)))
                .thenReturn(Collections.singletonList(stored));

        enrichmentService.enrichupdateRefundTransaction(refund);

        assertEquals("ATOM-1", refund.getAtomTxnId());
    }

    @Test(expected = CustomException.class)
    public void enrichupdateRefundTransactionMissingTxn() {
        when(transactionRepository.fetchTransactions(any(TransactionCriteria.class)))
                .thenReturn(Collections.emptyList());
        enrichmentService.enrichupdateRefundTransaction(Refund.builder().originalTxnId("missing").build());
    }

    @Test
    public void enrichRefundRequestBuildsRefund() {
        Transaction stored = Transaction.builder()
                .tenantId("pb")
                .txnId("TXN-10")
                .txnAmount("250")
                .atomTxnId("ATOM-10")
                .consumerCode("CC-10")
                .gateway("NTTDATA")
                .gatewayTxnId("GW-10")
                .build();
        when(idGenService.generateRefundId(any(RefundRequest.class))).thenReturn("RF-001");

        RefundRequest result = enrichmentService.enrichRefundRequest(Collections.singletonList(stored), requestInfo);

        assertEquals("RF-001", result.getRefund().getRefundId());
        assertEquals(RefundStatusEnum.INITIATED, result.getRefund().getStatus());
        assertEquals(new BigDecimal("250"), result.getRefund().getRefundAmount());
        verify(idGenService).generateRefundId(any(RefundRequest.class));
    }

    @Test
    public void enrichRefundTransactionBuildsPayload() {
        Refund refund = Refund.builder()
                .refundId("RF-99")
                .originalTxnId("TXN-99")
                .atomTxnId("ATOM-99")
                .refundAmount(new BigDecimal("100"))
                .build();

        RefundTransaction refundTransaction = enrichmentService.enrichRefundTransaction(refund, "MERCH-1");

        assertNotNull(refundTransaction.getPayInstrument());
        assertEquals("MERCH-1", refundTransaction.getPayInstrument().getMerchDetails().getMerchId());
        assertEquals("TXN-99", refundTransaction.getPayInstrument().getMerchDetails().getMerchTxnId());
    }

    @Test
    public void enrichRefundTransactionWithDecimalAmount() {
        Refund refund = Refund.builder()
                .refundId("RF-100")
                .originalTxnId("TXN-100")
                .atomTxnId("ATOM-100")
                .refundAmount(new BigDecimal("100.50"))
                .build();

        RefundTransaction refundTransaction = enrichmentService.enrichRefundTransaction(refund, "MERCH-2");

        assertNotNull(refundTransaction.getPayInstrument().getPayDetails().getSignature());
    }

    @Test
    public void enrichRefundStatusTransactionBuildsPayload() {
        Refund refund = Refund.builder().atomTxnId("ATOM-STATUS").build();

        RefundTransaction refundTransaction = enrichmentService.enrichRefundStatusTransaction(refund, "MERCH-3");

        assertEquals("REFUNDSTATUS", refundTransaction.getPayInstrument().getHeadDetails().getApi());
    }

    @Test(expected = CustomException.class)
    public void calculateTotalRefundAmountEmptyListFails() throws Exception {
        java.lang.reflect.Method method = EnrichmentService.class.getDeclaredMethod(
                "calculateTotalRefundAmount", List.class);
        method.setAccessible(true);
        try {
            method.invoke(enrichmentService, Collections.emptyList());
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    public void paymentRefundResponseCopiesFields() {
        Refund refund = Refund.builder()
                .refundId("RF-1")
                .originalTxnId("TXN-1")
                .status(RefundStatusEnum.SUCCESS)
                .gatewayStatusMsg("OK")
                .gatewayStatusCode("200")
                .build();
        CollectionPayment payment = CollectionPayment.builder().tenantId("pb").build();
        PaymentRefund paymentRefund = new PaymentRefund();

        enrichmentService.paymentRefundResponse(refund, paymentRefund, payment);

        assertEquals("pb", paymentRefund.getTenantId());
        assertEquals("RF-1", paymentRefund.getRefundId());
        assertEquals("TXN-1", paymentRefund.getTransactionId());
        assertEquals("SUCCESS", paymentRefund.getRefundStatus());
        assertEquals("OK", paymentRefund.getGatewayStausMsg());
    }
}
