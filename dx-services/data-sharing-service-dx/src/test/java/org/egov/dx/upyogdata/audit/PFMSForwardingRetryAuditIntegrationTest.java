package org.egov.dx.upyogdata.audit;

import feign.FeignException;
import feign.Request;
import org.egov.dx.upyogdata.audit.support.ExternalApiAuditTableHarness;
import org.egov.dx.upyogdata.constants.Constants;
import org.egov.dx.upyogdata.pfms.client.PFMSApiClient;
import org.egov.dx.upyogdata.pfms.client.PFMSAuthFeignClient;
import org.egov.dx.upyogdata.pfms.client.PFMSDataFeignClient;
import org.egov.dx.upyogdata.pfms.models.PFMSAuthResponse;
import org.egov.dx.upyogdata.pfms.models.PFMSTransaction;
import org.egov.dx.upyogdata.pfms.repository.PFMSRepository;
import org.egov.dx.upyogdata.pfms.service.PFMSForwardingServiceImpl;
import org.egov.externalaudit.constants.ExternalApiAuditConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PFMSForwardingRetryAuditIntegrationTest {

    private static ExternalApiAuditTableHarness harness;

    private PFMSAuthFeignClient authFeignClient;
    private PFMSDataFeignClient dataFeignClient;
    private PFMSRepository pfmsRepository;
    private PFMSForwardingServiceImpl forwardingService;

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
        pfmsRepository = mock(PFMSRepository.class);
        PFMSApiClient apiClient = new PFMSApiClient(authFeignClient, dataFeignClient,
                harness.newLogger("upyog-data-dx", true));
        ReflectionTestUtils.setField(apiClient, "username", "pfms-user");
        ReflectionTestUtils.setField(apiClient, "password", "pfms-password-secret");
        ReflectionTestUtils.setField(apiClient, "clientIp", "10.0.0.8");
        ReflectionTestUtils.setField(apiClient, "authUrl", "http://pfms.example/auth");
        ReflectionTestUtils.setField(apiClient, "dataUrl", "http://pfms.example/data");
        forwardingService = new PFMSForwardingServiceImpl(pfmsRepository, apiClient);
        ReflectionTestUtils.setField(forwardingService, "batchSize", 50);
    }

    @Test
    void unauthorizedPushRetry_updatesSameDataPushRow() {
        PFMSTransaction transaction = sampleTransaction("txn-1", "VCH-1");
        when(pfmsRepository.fetchInitiatedTransactionIds(50)).thenReturn(List.of("txn-1"));
        when(pfmsRepository.fetchTransactionById("txn-1")).thenReturn(transaction);
        when(authFeignClient.authenticate(any())).thenReturn(auth("token-1"), auth("token-2"));
        when(dataFeignClient.pushTransaction(eq("Bearer token-1"), any())).thenThrow(unauthorized());
        when(dataFeignClient.pushTransaction(eq("Bearer token-2"), any())).thenReturn("SUCCESS");

        forwardingService.forwardInitiatedTransactions(Constants.MANUAL);

        List<Map<String, Object>> authRows = harness.messagesByApi(ExternalApiAuditConstants.API_PFMS_AUTH);
        List<Map<String, Object>> pushRows = harness.messagesByApi(ExternalApiAuditConstants.API_PFMS_DATA_PUSH);

        assertEquals(2, authRows.size());
        assertNotEquals(authRows.get(0).get("correlation_id"), authRows.get(1).get("correlation_id"));
        assertEquals(ExternalApiAuditConstants.STATUS_SUCCESS, authRows.get(0).get("status"));
        assertEquals(ExternalApiAuditConstants.STATUS_SUCCESS, authRows.get(1).get("status"));

        assertEquals(1, pushRows.size());
        Map<String, Object> push = pushRows.get(0);
        assertEquals(ExternalApiAuditConstants.STATUS_SUCCESS, push.get("status"));
        assertEquals(1, ((Number) push.get("retry_count")).intValue());
        assertEquals("PFMS", push.get("tenant_id"));
        assertEquals(ExternalApiAuditConstants.DIRECTION_OUTBOUND, push.get("direction"));
        assertEquals(1, harness.errors((String) push.get("correlation_id")).size());
        assertEquals("HTTP_CLIENT_ERROR", harness.errors((String) push.get("correlation_id")).get(0).get("error_code"));
    }

    @Test
    void secondSchedulerCycle_createsANewDataPushRow() {
        PFMSTransaction transaction = sampleTransaction("txn-2", "VCH-2");
        when(pfmsRepository.fetchInitiatedTransactionIds(50)).thenReturn(List.of("txn-2"));
        when(pfmsRepository.fetchTransactionById("txn-2")).thenReturn(transaction);
        when(authFeignClient.authenticate(any())).thenReturn(auth("token"));
        when(dataFeignClient.pushTransaction(anyString(), any())).thenReturn("SUCCESS");

        forwardingService.forwardInitiatedTransactions(Constants.MANUAL);
        forwardingService.forwardInitiatedTransactions(Constants.MANUAL);

        List<Map<String, Object>> pushRows = harness.messagesByApi(ExternalApiAuditConstants.API_PFMS_DATA_PUSH);
        assertEquals(2, pushRows.size());
        assertNotEquals(pushRows.get(0).get("correlation_id"), pushRows.get(1).get("correlation_id"));
        assertEquals(0, ((Number) pushRows.get(0).get("retry_count")).intValue());
        assertEquals(0, ((Number) pushRows.get(1).get("retry_count")).intValue());
        assertEquals(ExternalApiAuditConstants.STATUS_SUCCESS, pushRows.get(0).get("status"));
        assertEquals(ExternalApiAuditConstants.STATUS_SUCCESS, pushRows.get(1).get("status"));
    }

    @Test
    void nonUnauthorizedPushFailure_writesFailedRowWithoutRetry() {
        PFMSTransaction transaction = sampleTransaction("txn-3", "VCH-3");
        when(pfmsRepository.fetchFailedTransactionIds(50)).thenReturn(List.of("txn-3"));
        when(pfmsRepository.fetchTransactionById("txn-3")).thenReturn(transaction);
        when(authFeignClient.authenticate(any())).thenReturn(auth("token"));
        when(dataFeignClient.pushTransaction(anyString(), any())).thenThrow(serverError());

        forwardingService.retryFailedTransactions(Constants.MANUAL);

        List<Map<String, Object>> pushRows = harness.messagesByApi(ExternalApiAuditConstants.API_PFMS_DATA_PUSH);
        assertEquals(1, pushRows.size());
        assertEquals(ExternalApiAuditConstants.STATUS_FAILED, pushRows.get(0).get("status"));
        assertEquals(0, ((Number) pushRows.get(0).get("retry_count")).intValue());
        assertEquals(502, ((Number) pushRows.get(0).get("http_status_code")).intValue());
        assertEquals(1, harness.messagesByApi(ExternalApiAuditConstants.API_PFMS_AUTH).size());
    }

    private PFMSAuthResponse auth(String token) {
        PFMSAuthResponse response = new PFMSAuthResponse();
        response.setAccessToken(token);
        return response;
    }

    private PFMSTransaction sampleTransaction(String id, String voucherNumber) {
        PFMSTransaction transaction = new PFMSTransaction();
        transaction.setId(id);
        transaction.setCorrelationId("batch-" + id);
        transaction.setUlbCodeOrPFMSAgencyCode("ULB1");
        transaction.setVoucherNumber(voucherNumber);
        transaction.setVoucherDate(LocalDate.of(2026, 10, 5));
        transaction.setVoucherType("Payment Voucher");
        transaction.setFinancialYear("2026");
        transaction.setAccountHeadCode("AH1");
        transaction.setFunctionCode("FN1");
        transaction.setSchemeCode("SC1");
        return transaction;
    }

    private FeignException unauthorized() {
        return feignStatus(401, "Unauthorized");
    }

    private FeignException serverError() {
        return feignStatus(502, "Bad Gateway");
    }

    private FeignException feignStatus(int status, String reason) {
        Request request = Request.create(Request.HttpMethod.POST, "http://pfms.example/data",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        return FeignException.errorStatus("pushTransaction",
                feign.Response.builder()
                        .status(status)
                        .reason(reason)
                        .request(request)
                        .headers(Collections.emptyMap())
                        .body(reason, StandardCharsets.UTF_8)
                        .build());
    }
}
