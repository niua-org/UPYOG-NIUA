package org.egov.pg.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pg.config.AppProperties;
import org.egov.pg.models.IdGenerationResponse;
import org.egov.pg.models.IdResponse;
import org.egov.pg.models.Refund;
import org.egov.pg.models.RefundRequest;
import org.egov.pg.models.Transaction;
import org.egov.pg.repository.IdGenRepository;
import org.egov.pg.repository.TransactionRepository;
import org.egov.pg.web.models.TransactionRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IdGenServiceTest {

    @Mock
    private IdGenRepository idGenRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AppProperties appProperties;

    private IdGenService idGenService;

    @Before
    public void setUp() {
        idGenService = new IdGenService(idGenRepository, transactionRepository, appProperties);
        when(appProperties.getIdGenName()).thenReturn("pg.txnid");
        when(appProperties.getIdGenFormat()).thenReturn("PG_{seq}");
        when(appProperties.getIdGenRefundName()).thenReturn("pg.refund");
        when(appProperties.getIdGenRefundFormat()).thenReturn("RF_{seq}");
    }

    @Test
    public void generateTxnIdReturnsIdFromRepository() {
        RequestInfo requestInfo = new RequestInfo();
        Transaction transaction = Transaction.builder().tenantId("pb").build();
        TransactionRequest request = new TransactionRequest(requestInfo, transaction);
        IdGenerationResponse response = new IdGenerationResponse(null,
                Collections.singletonList(new IdResponse("TXN-ID-001")));
        when(idGenRepository.getId(eq(requestInfo), eq("pb"), eq("pg.txnid"), eq("PG_{seq}"), eq(1)))
                .thenReturn(response);

        assertEquals("TXN-ID-001", idGenService.generateTxnId(request));
    }

    @Test
    public void generateRefundIdReturnsIdFromRepository() {
        RequestInfo requestInfo = new RequestInfo();
        Refund refund = Refund.builder().tenantId("pb").build();
        RefundRequest refundRequest = new RefundRequest(requestInfo, refund);
        IdGenerationResponse response = new IdGenerationResponse(null,
                Collections.singletonList(new IdResponse("RF-ID-001")));
        when(idGenRepository.getId(eq(requestInfo), eq("pb"), eq("pg.refund"), eq("RF_{seq}"), eq(1)))
                .thenReturn(response);

        assertEquals("RF-ID-001", idGenService.generateRefundId(refundRequest));
    }
}
