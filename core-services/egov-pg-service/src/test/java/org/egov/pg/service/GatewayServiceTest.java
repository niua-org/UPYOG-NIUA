package org.egov.pg.service;

import org.egov.pg.constants.PgConstants;
import org.egov.pg.models.Refund;
import org.egov.pg.models.Transaction;
import org.egov.tracer.model.CustomException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GatewayServiceTest {

    private static final String ACTIVE_GATEWAY = "MOCKGW";

    @Mock
    private Gateway activeGateway;

    @Mock
    private Gateway inactiveGateway;

    private GatewayService gatewayService;

    @Before
    public void setUp() {
        when(activeGateway.gatewayName()).thenReturn(ACTIVE_GATEWAY);
        when(activeGateway.isActive()).thenReturn(true);
        when(activeGateway.transactionIdKeyInResponse()).thenReturn("mock_txn_id");

        when(inactiveGateway.gatewayName()).thenReturn("INACTIVEGW");
        when(inactiveGateway.isActive()).thenReturn(false);
        when(inactiveGateway.transactionIdKeyInResponse()).thenReturn("inactive_txn_id");

        gatewayService = new GatewayService(Arrays.asList(activeGateway, inactiveGateway));
    }

    @Test
    public void getActiveGatewaysContainsRegisteredGateways() {
        Set<String> gateways = gatewayService.getActiveGateways();

        assertTrue(gateways.contains(ACTIVE_GATEWAY));
        assertTrue(gateways.contains("INACTIVEGW"));
    }

    @Test
    public void isGatewayActiveReflectsGatewayBeanState() {
        assertTrue(gatewayService.isGatewayActive(ACTIVE_GATEWAY));
        assertFalse(gatewayService.isGatewayActive("INACTIVEGW"));
        assertFalse(gatewayService.isGatewayActive("UNKNOWN"));
    }

    @Test
    public void getTxnIdFromPgLabelCaseInsensitive() {
        Map<String, String> params = new HashMap<>();
        params.put(PgConstants.PG_TXN_IN_LABEL.toUpperCase(), "txn-from-callback");

        Optional<String> txnId = gatewayService.getTxnId(params);

        assertTrue(txnId.isPresent());
        assertEquals("txn-from-callback", txnId.get());
    }

    @Test
    public void getTxnIdFromGatewaySpecificKey() {
        Map<String, String> params = new HashMap<>();
        params.put("MOCK_TXN_ID", "gateway-txn-99");

        Optional<String> txnId = gatewayService.getTxnId(params);

        assertTrue(txnId.isPresent());
        assertEquals("gateway-txn-99", txnId.get());
    }

    @Test
    public void getTxnIdEmptyWhenNoKnownKeys() {
        assertFalse(gatewayService.getTxnId(Collections.singletonMap("unknown", "x")).isPresent());
    }

    @Test
    public void initiateTxnSuccessForActiveGateway() throws Exception {
        Transaction transaction = Transaction.builder().gateway(ACTIVE_GATEWAY).build();
        URI redirect = new URI("https://pay.example/redirect");
        when(activeGateway.generateRedirectURI(transaction)).thenReturn(redirect);

        URI result = gatewayService.initiateTxn(transaction);

        assertEquals(redirect, result);
        verify(activeGateway).generateRedirectURI(transaction);
    }

    @Test(expected = CustomException.class)
    public void initiateTxnFailsForInactiveGateway() {
        Transaction transaction = Transaction.builder().gateway("INACTIVEGW").build();

        try {
            gatewayService.initiateTxn(transaction);
        } catch (CustomException e) {
            assertEquals("INVALID_PAYMENT_GATEWAY", e.getCode());
            throw e;
        }
    }

    @Test(expected = CustomException.class)
    public void initiateTxnFailsForUnknownGateway() {
        gatewayService.initiateTxn(Transaction.builder().gateway("MISSING").build());
    }

    @Test
    public void getLiveStatusDelegatesToGateway() {
        Transaction current = Transaction.builder().gateway(ACTIVE_GATEWAY).build();
        Transaction updated = Transaction.builder().gateway(ACTIVE_GATEWAY).txnId("done").build();
        Map<String, String> params = Collections.singletonMap("status", "SUCCESS");
        when(activeGateway.fetchStatus(current, params)).thenReturn(updated);

        Transaction result = gatewayService.getLiveStatus(current, params);

        assertEquals(updated, result);
        verify(activeGateway).fetchStatus(current, params);
    }

    @Test
    public void initiateRefundSuccessForActiveGateway() {
        Refund refund = Refund.builder().gateway(ACTIVE_GATEWAY).refundId("RF-1").build();
        Refund gatewayResponse = Refund.builder().gateway(ACTIVE_GATEWAY).refundId("RF-1").build();
        when(activeGateway.initiateRefund(refund)).thenReturn(gatewayResponse);

        Refund result = gatewayService.initiateRefund(refund);

        assertEquals(gatewayResponse, result);
        verify(activeGateway).initiateRefund(refund);
    }

    @Test(expected = CustomException.class)
    public void initiateRefundFailsForInactiveGateway() {
        Refund refund = Refund.builder().gateway("INACTIVEGW").build();

        try {
            gatewayService.initiateRefund(refund);
        } catch (CustomException e) {
            assertEquals("INVALID_PAYMENT_GATEWAY", e.getCode());
            throw e;
        }
    }

    @Test
    public void getRefundLiveStatusDelegatesToGateway() {
        Refund current = Refund.builder().gateway(ACTIVE_GATEWAY).refundId("RF-2").build();
        Refund updated = Refund.builder().gateway(ACTIVE_GATEWAY).refundId("RF-2").build();
        when(activeGateway.fetchRefundStatus(current)).thenReturn(updated);

        Refund result = gatewayService.getRefundLiveStatus(current);

        assertEquals(updated, result);
        verify(activeGateway).fetchRefundStatus(current);
    }

    @Test
    public void emptyGatewayListStillResolvesPgTxnLabel() {
        GatewayService emptyService = new GatewayService(Collections.emptyList());

        Optional<String> txnId = emptyService.getTxnId(
                Collections.singletonMap(PgConstants.PG_TXN_IN_LABEL, "only-label"));

        assertTrue(txnId.isPresent());
        assertEquals("only-label", txnId.get());
        assertTrue(emptyService.getActiveGateways().isEmpty());
    }
}
