package org.egov.dx.upyogdata.audit;

import org.egov.dx.upyogdata.audit.support.ExternalApiAuditTableHarness;
import org.egov.dx.upyogdata.pfms.client.PFMSApiClient;
import org.egov.dx.upyogdata.pfms.client.PFMSAuthFeignClient;
import org.egov.dx.upyogdata.pfms.client.PFMSDataFeignClient;
import org.egov.dx.upyogdata.pfms.models.PFMSAuthResponse;
import org.egov.dx.upyogdata.pfms.models.PFMSTransaction;
import org.egov.externalaudit.constants.ExternalApiAuditConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PFMSOutboundAuditIntegrationTest {

    private static ExternalApiAuditTableHarness harness;

    private PFMSAuthFeignClient authFeignClient;
    private PFMSDataFeignClient dataFeignClient;
    private PFMSApiClient client;

    @BeforeAll
    static void startDatabase() {
        harness = ExternalApiAuditTableHarness.start();
    }

    @AfterAll
    static void stopDatabase() {
        harness.close();
    }

    @BeforeEach
    void setUp() {
        harness.truncate();
        authFeignClient = mock(PFMSAuthFeignClient.class);
        dataFeignClient = mock(PFMSDataFeignClient.class);
        client = new PFMSApiClient(authFeignClient, dataFeignClient, harness.newLogger("upyog-data-dx", true));
        ReflectionTestUtils.setField(client, "username", "pfms-user");
        ReflectionTestUtils.setField(client, "password", "pfms-password-secret");
        ReflectionTestUtils.setField(client, "clientIp", "10.0.0.8");
        ReflectionTestUtils.setField(client, "authUrl", "http://pfms.example/auth");
        ReflectionTestUtils.setField(client, "dataUrl", "http://pfms.example/data");
    }

    @Test
    void pfmsAuthSuccess_masksPasswordAndTokens() {
        PFMSAuthResponse authResponse = new PFMSAuthResponse();
        authResponse.setAccessToken("live-access-token");
        authResponse.setRefreshToken("live-refresh-token");
        when(authFeignClient.authenticate(any())).thenReturn(authResponse);

        String token = client.fetchAccessToken();

        assertEquals("live-access-token", token);
        assertEquals(1, harness.messageCount());
        String correlationId = harness.onlyCorrelationId();
        Map<String, Object> message = harness.message(correlationId);
        assertEquals("PFMS", message.get("tenant_id"));
        assertEquals("upyog-data-dx", message.get("state"));
        assertEquals(ExternalApiAuditConstants.API_PFMS_AUTH, message.get("external_api_name"));
        assertEquals(ExternalApiAuditConstants.DIRECTION_OUTBOUND, message.get("direction"));
        assertEquals(ExternalApiAuditConstants.STATUS_SUCCESS, message.get("status"));

        Map<String, Object> raw = harness.raw(correlationId);
        String requestPayload = String.valueOf(raw.get("request_payload"));
        String responsePayload = String.valueOf(raw.get("response_payload"));
        assertTrue(requestPayload.contains("********"));
        assertFalse(requestPayload.contains("pfms-password-secret"));
        assertTrue(responsePayload.contains("********"));
        assertFalse(responsePayload.contains("live-access-token"));
        assertFalse(responsePayload.contains("live-refresh-token"));
    }

    @Test
    void pfmsAuthMissingToken_writesFailedRow() {
        when(authFeignClient.authenticate(any())).thenReturn(new PFMSAuthResponse());

        RuntimeException thrown = assertThrows(RuntimeException.class, client::fetchAccessToken);

        assertTrue(thrown.getMessage().contains("token missing"));
        assertEquals(1, harness.messageCount());
        assertEquals(1, harness.errorCount());
        String correlationId = harness.onlyCorrelationId();
        assertEquals(ExternalApiAuditConstants.STATUS_FAILED, harness.message(correlationId).get("status"));
        assertEquals("INTEGRATION_CALL_FAILED", harness.errors(correlationId).get(0).get("error_code"));
    }

    @Test
    void pfmsDataPushSuccess_masksBankAccountsAndKeepsBusinessIds() {
        when(dataFeignClient.pushTransaction(anyString(), any())).thenReturn("SUCCESS");

        String result = client.pushTransaction(sampleTransaction(), "token", null, 0);

        assertEquals("SUCCESS", result);
        String correlationId = harness.onlyCorrelationId();
        Map<String, Object> message = harness.message(correlationId);
        assertEquals(ExternalApiAuditConstants.API_PFMS_DATA_PUSH, message.get("external_api_name"));
        assertEquals(ExternalApiAuditConstants.DIRECTION_OUTBOUND, message.get("direction"));
        assertEquals("PFMS", message.get("tenant_id"));
        assertEquals(ExternalApiAuditConstants.STATUS_SUCCESS, message.get("status"));
        assertNotEquals("batch-corr-9", correlationId);

        String requestPayload = String.valueOf(harness.raw(correlationId).get("request_payload"));
        assertTrue(requestPayload.contains("batch-corr-9"));
        assertTrue(requestPayload.contains("txn-42"));
        assertTrue(requestPayload.contains("********"));
        assertFalse(requestPayload.contains("111122223333"));
        assertFalse(requestPayload.contains("444455556666"));
        assertFalse(requestPayload.contains("from-acc-secret"));
        assertFalse(requestPayload.contains("to-acc-secret"));
        assertTrue(requestPayload.contains("VCH-42"));
    }

    @Test
    void pfmsDataPushServerError_writesErrorDetail() {
        when(dataFeignClient.pushTransaction(anyString(), any())).thenThrow(
                HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "Bad Gateway", null,
                        "pfms down".getBytes(), null));

        assertThrows(HttpServerErrorException.class,
                () -> client.pushTransaction(sampleTransaction(), "token"));

        String correlationId = harness.onlyCorrelationId();
        Map<String, Object> message = harness.message(correlationId);
        assertEquals(ExternalApiAuditConstants.STATUS_FAILED, message.get("status"));
        assertEquals(502, ((Number) message.get("http_status_code")).intValue());
        Map<String, Object> error = harness.errors(correlationId).get(0);
        assertEquals("HTTP_SERVER_ERROR", error.get("error_code"));
        assertEquals(ExternalApiAuditConstants.ERROR_TYPE_SERVER, error.get("error_type"));
        assertEquals("pfms down", error.get("error_message"));
    }

    @Test
    void twoSchedulerCycles_writeTwoDataPushRows() {
        when(dataFeignClient.pushTransaction(anyString(), any())).thenReturn("SUCCESS");

        client.pushTransaction(sampleTransaction(), "token");
        client.pushTransaction(sampleTransaction(), "token");

        List<Map<String, Object>> rows = harness.messagesByApi(ExternalApiAuditConstants.API_PFMS_DATA_PUSH);
        assertEquals(2, rows.size());
        assertNotEquals(rows.get(0).get("correlation_id"), rows.get(1).get("correlation_id"));
        assertEquals(0, ((Number) rows.get(0).get("retry_count")).intValue());
        assertEquals(0, ((Number) rows.get(1).get("retry_count")).intValue());
    }

    private PFMSTransaction sampleTransaction() {
        PFMSTransaction transaction = new PFMSTransaction();
        transaction.setId("txn-42");
        transaction.setCorrelationId("batch-corr-9");
        transaction.setUlbCodeOrPFMSAgencyCode("ULB1");
        transaction.setVoucherNumber("VCH-42");
        transaction.setVoucherDate(LocalDate.of(2026, 10, 5));
        transaction.setVoucherType("Payment Voucher");
        transaction.setFinancialYear("2026");
        transaction.setAccountHeadCode("AH1");
        transaction.setFunctionCode("FN1");
        transaction.setSchemeCode("SC1");
        transaction.setUlbBankAccountNumber("111122223333");
        transaction.setBeneficiaryAccountNumber("444455556666");
        transaction.setFromAccount("from-acc-secret");
        transaction.setToAccount("to-acc-secret");
        return transaction;
    }
}
