package org.egov.pg.validator;

import org.egov.pg.models.Refund;
import org.egov.pg.models.Refund.RefundStatusEnum;
import org.egov.pg.models.Transaction;
import org.egov.pg.repository.RefundRepository;
import org.egov.pg.web.models.RefundCriteria;
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RefundValidatorTest {

    @Mock
    private RefundRepository repository;

    private RefundValidator validator;

    @Before
    public void setUp() {
        validator = new RefundValidator(repository);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void validateTransactionEmptyListFailsFast() {
        validator.validateTransaction(Collections.emptyList());
    }

    @Test
    public void validateTransactionRunsFieldChecks() {
        Transaction txn = Transaction.builder().txnId("").build();
        when(repository.fetchRefundTransactions(any(RefundCriteria.class))).thenReturn(Collections.emptyList());
        validator.validateTransaction(Collections.singletonList(txn));
    }

    @Test
    public void validateTransactionChecksExistingRefunds() {
        Transaction txn = validTransaction();
        Refund existing = Refund.builder().status(RefundStatusEnum.PENDING).build();
        when(repository.fetchRefundTransactions(any(RefundCriteria.class))).thenReturn(Collections.singletonList(existing));
        validator.validateTransaction(Collections.singletonList(txn));
    }

    @Test
    public void validateUpdateRefundTransactionSuccess() {
        Refund refund = Refund.builder().refundId("RF-1").build();
        when(repository.fetchRefundTransactions(any(RefundCriteria.class)))
                .thenReturn(Collections.singletonList(refund));

        Refund result = validator.validateUpdateRefundTransaction(Collections.singletonMap("refundId", "RF-1"));

        assertSame(refund, result);
    }

    @Test(expected = CustomException.class)
    public void validateUpdateRefundTransactionMissingId() {
        validator.validateUpdateRefundTransaction(new HashMap<>());
    }

    @Test
    public void skipGatewayWhenZeroRefund() {
        assertTrue(validator.skipGateway(Refund.builder().refundAmount(BigDecimal.ZERO).build()));
        assertFalse(validator.skipGateway(Refund.builder().refundAmount(BigDecimal.TEN).build()));
    }

    private Transaction validTransaction() {
        return Transaction.builder()
                .txnId("TXN-1")
                .gatewayTxnId("GW-1")
                .consumerCode("CC-1")
                .billId("BILL-1")
                .build();
    }
}
