package org.egov.dx.upyogdata.pfms.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egov.dx.upyogdata.pfms.models.PFMSAuthRequest;
import org.egov.dx.upyogdata.pfms.models.PFMSAuthResponse;
import org.egov.dx.upyogdata.pfms.models.PFMSTransaction;
import org.egov.dx.upyogdata.pfms.models.PFMSTransactionFormData;
import org.egov.externalaudit.constants.ExternalApiAuditConstants;
import org.egov.externalaudit.model.ExternalIntegrationContext;
import org.egov.externalaudit.service.ExternalApiAuditLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Handles all outbound HTTP calls to the PFMS external API.
 * Uses Feign clients internally for auth and data push operations.
 * Each call is explicitly audited via {@link ExternalApiAuditLogger}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PFMSApiClient {

    private static final String PFMS_TENANT = "PFMS";

    private final PFMSAuthFeignClient authFeignClient;
    private final PFMSDataFeignClient dataFeignClient;
    private final ExternalApiAuditLogger auditLogger;

    @Value("${pfms.auth.username}")
    private String username;

    @Value("${pfms.auth.password}")
    private String password;

    @Value("${pfms.client.ip}")
    private String clientIp;

    @Value("${pfms.auth.url}")
    private String authUrl;

    @Value("${pfms.data.url}")
    private String dataUrl;

    /**
     * Authenticates with PFMS and returns a Bearer access token.
     * Throws RuntimeException if the response is null or token is missing.
     *
     * @return Bearer token string to use in subsequent API calls
     */
    public String fetchAccessToken() {
        log.info("Fetching access token from PFMS auth API");
        PFMSAuthRequest authRequest = new PFMSAuthRequest(username, password);
        PFMSAuthResponse authResponse = auditLogger.logAndExecute(ExternalIntegrationContext.builder()
                .tenantId(PFMS_TENANT)
                .externalApiName(ExternalApiAuditConstants.API_PFMS_AUTH)
                .requestPayload(authRequest)
                .endpoint(authUrl)
                .httpMethod("POST")
                .build(), () -> {
            PFMSAuthResponse response = authFeignClient.authenticate(authRequest);
            if (response == null || response.getAccessToken() == null) {
                throw new RuntimeException("PFMS auth response is null or token missing");
            }
            return response;
        });
        log.info("Pfms Access token fetched successfully");
        return authResponse.getAccessToken();
    }

    /**
     * Converts the transaction to form data and pushes it to PFMS.
     * Returns the raw response string from PFMS as-is.
     *
     * @param transaction the transaction to push
     * @param accessToken Bearer token from fetchAccessToken()
     * @return raw response body from PFMS
     */
    public String pushTransaction(PFMSTransaction transaction, String accessToken) {
        return pushTransaction(transaction, accessToken, null, 0);
    }

    /**
     * Pushes a transaction using an explicit audit correlation id so an in-cycle
     * retry (e.g. after 401) updates the same audit row.
     */
    public String pushTransaction(PFMSTransaction transaction, String accessToken,
            String auditCorrelationId, int retryCount) {
        log.info("Pushing transaction to Pfms | voucherNumber={}", transaction.getVoucherNumber());
        PFMSTransactionFormData formData = PFMSTransactionFormData.from(transaction, clientIp);
        String response = auditLogger.logAndExecute(ExternalIntegrationContext.builder()
                .correlationId(auditCorrelationId)
                .retryCount(retryCount)
                .tenantId(PFMS_TENANT)
                .externalApiName(ExternalApiAuditConstants.API_PFMS_DATA_PUSH)
                .requestPayload(formData)
                .originatingCorrelationId(transaction.getCorrelationId())
                .businessReferenceId(transaction.getId())
                .endpoint(dataUrl)
                .httpMethod("POST")
                .build(), () -> dataFeignClient.pushTransaction("Bearer " + accessToken, formData));
        log.info("Pfms Response | voucherNumber={} body={}", transaction.getVoucherNumber(), response);
        return response;
    }
}
