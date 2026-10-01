package org.egov.dx.upyogdata.pfms.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egov.dx.upyogdata.pfms.models.PFMSAuthRequest;
import org.egov.dx.upyogdata.pfms.models.PFMSAuthResponse;
import org.egov.dx.upyogdata.pfms.models.PFMSTransaction;
import org.egov.dx.upyogdata.pfms.models.PFMSTransactionFormData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Handles all outbound HTTP calls to the PFMS external API.
 * Uses Feign clients internally for auth and data push operations.
 */


@Slf4j
@Component
@RequiredArgsConstructor
public class PFMSApiClient {

    private final PFMSAuthFeignClient authFeignClient;
    private final PFMSDataFeignClient dataFeignClient;

    @Value("${pfms.auth.username}")
    private String username;

    @Value("${pfms.auth.password}")
    private String password;

    @Value("${pfms.client.ip}")
    private String clientIp;

    /**
     * Authenticates with PFMS and returns a Bearer access token.
     * Throws RuntimeException if the response is null or token is missing.
     *
     * @return Bearer token string to use in subsequent API calls
     */
    public String fetchAccessToken() {
        log.info("Fetching access token from PFMS auth API");
        PFMSAuthResponse response = authFeignClient.authenticate(new PFMSAuthRequest(username, password));
        if (response == null || response.getAccessToken() == null)
            throw new RuntimeException("PFMS auth response is null or token missing");
        log.info("Pfms Access token fetched successfully");
        return response.getAccessToken();
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
        log.info("Pushing transaction to Pfms | voucherNumber={}", transaction.getVoucherNumber());
        PFMSTransactionFormData formData = PFMSTransactionFormData.from(transaction, clientIp);
        String response = dataFeignClient.pushTransaction("Bearer " + accessToken, formData);
        log.info("Pfms Response | voucherNumber={} body={}", transaction.getVoucherNumber(), response);
        return response;
    }
}
