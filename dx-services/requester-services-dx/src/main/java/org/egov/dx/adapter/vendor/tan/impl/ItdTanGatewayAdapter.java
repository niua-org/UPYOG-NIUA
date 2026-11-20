package org.egov.dx.adapter.vendor.tan.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.dx.adapter.vendor.dto.VendorTanVerificationRequest;
import org.egov.dx.adapter.vendor.dto.VendorTanVerificationResponse;
import org.egov.dx.adapter.vendor.tan.TanGatewayAdapter;
import org.egov.dx.util.Configurations;
import org.egov.dx.util.TanConstants;
import org.egov.dx.util.TanNumberUtils;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

/**
 * Production implementation of {@link TanGatewayAdapter} connecting to the official
 * Income Tax Department (ITD) e-Filing External Agency Web Service.
 *
 * <p>Strictly satisfies SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles remote HTTP transport and response mapping for live ITD gateway.</li>
 *   <li><b>Liskov Substitution Principle (LSP):</b> Seamlessly substitutes {@link TanGatewayAdapter}.</li>
 * </ul>
 *
 * <p>Requires all properties to be explicitly defined in {@code application.properties} with no silent hardcoded fallbacks.</p>
 * <p>Activated when {@code tan.verification.mock.enabled=false}.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "tan.verification.mock.enabled", havingValue = "false")
@Slf4j
public class ItdTanGatewayAdapter implements TanGatewayAdapter {

    private final Configurations configurations;
    private final RestClient restClient;

    /**
     * Constructs the adapter and configures resilient connection and read timeouts.
     *
     * @param configurations application configurations
     */
    @Autowired
    public ItdTanGatewayAdapter(Configurations configurations) {
        this.configurations = configurations;

        // Startup sanity check: verify mandatory HTTP connect timeout configuration
        if (configurations.getTanVerificationConnectTimeout() == null) {
            throw new CustomException(TanConstants.ERR_CODE_INVALID_REQUEST, "tan.verification.connect.timeout is not configured");
        }
        // Startup sanity check: verify mandatory HTTP socket read timeout configuration
        if (configurations.getTanVerificationReadTimeout() == null) {
            throw new CustomException(TanConstants.ERR_CODE_INVALID_REQUEST, "tan.verification.read.timeout is not configured");
        }
        // Startup sanity check: verify mandatory ITD vendor endpoint base URL
        if (StringUtils.isBlank(configurations.getTanVerificationBaseUrl())) {
            throw new CustomException(TanConstants.ERR_CODE_INVALID_REQUEST, "tan.verification.base.url is not configured");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(configurations.getTanVerificationConnectTimeout());
        requestFactory.setReadTimeout(configurations.getTanVerificationReadTimeout());

        this.restClient = RestClient.builder()
                .baseUrl(configurations.getTanVerificationBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Submits outbound TAN verification request to ITD web service and maps vendor response.</p>
     */
    @Override
    public VendorTanVerificationResponse verifyTan(VendorTanVerificationRequest request) {
        String path = configurations.getTanVerificationPath();
        // Endpoint path verification: ensure target URI path is configured
        if (StringUtils.isBlank(path)) {
            throw new CustomException(TanConstants.ERR_CODE_INVALID_REQUEST, "tan.verification.path is not configured");
        }
        // Credential validation: ensure client ID is present
        if (StringUtils.isBlank(configurations.getTanVerificationClientId())) {
            throw new CustomException(TanConstants.ERR_CODE_INVALID_REQUEST, "tan.verification.client.id is not configured");
        }
        // Credential validation: ensure client secret is present
        if (StringUtils.isBlank(configurations.getTanVerificationClientSecret())) {
            throw new CustomException(TanConstants.ERR_CODE_INVALID_REQUEST, "tan.verification.client.secret is not configured");
        }

        String rawTan = request != null && request.getSearchCriteria() != null
                ? request.getSearchCriteria().getTan()
                : null;
        String maskedTan = TanNumberUtils.maskTan(rawTan);

        log.info("[LIVE-TAN] Invoking ITD TAN verification endpoint [{}{}] for masked TAN: {}",
                configurations.getTanVerificationBaseUrl(), path, maskedTan);

        try {
            VendorTanVerificationResponse response = restClient.post()
                    .uri(path)
                    .header("Client-Id", configurations.getTanVerificationClientId().trim())
                    .header("Client-Secret", configurations.getTanVerificationClientSecret().trim())
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (clientRequest, clientResponse) -> {
                        log.error("[LIVE-TAN] ITD Gateway returned HTTP error {} for masked TAN {}",
                                clientResponse.getStatusCode(), maskedTan);
                        // Security & PII hygiene (Review comment Medium #2): Only capture full error payload at DEBUG level
                        if (log.isDebugEnabled()) {
                            String responseBody = new String(clientResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            log.debug("[LIVE-TAN] ITD Gateway error payload for masked TAN {}: {}", maskedTan, responseBody);
                        }
                        throw new CustomException(TanConstants.ERR_CODE_GATEWAY_ERROR,
                                "ITD TAN gateway failed with status " + clientResponse.getStatusCode());
                    })
                    .body(VendorTanVerificationResponse.class);

            // Null response guard: fail safely if gateway returns 200 OK with empty body
            if (response == null) {
                log.error("[LIVE-TAN] Empty response body from ITD Gateway for masked TAN: {}", maskedTan);
                throw new CustomException(TanConstants.ERR_CODE_GATEWAY_ERROR, TanConstants.MSG_GATEWAY_ERROR);
            }

            log.info("[LIVE-TAN] ITD Gateway successfully processed masked TAN: {}, status: {}",
                    maskedTan, response.getHeader() != null ? response.getHeader().getStatus() : "null");
            return response;

        } catch (CustomException ce) {
            throw ce;
        } catch (Exception ex) {
            log.error("[LIVE-TAN] Unexpected transport exception for masked TAN {}: {}", maskedTan, ex.getMessage(), ex);
            throw new CustomException(TanConstants.ERR_CODE_GATEWAY_ERROR,
                    "Failed to communicate with ITD TAN gateway: " + ex.getMessage());
        }
    }
}
