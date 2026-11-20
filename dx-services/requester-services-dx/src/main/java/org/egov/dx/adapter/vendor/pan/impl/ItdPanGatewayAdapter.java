package org.egov.dx.adapter.vendor.pan.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.dx.adapter.vendor.dto.VendorPanVerificationRequest;
import org.egov.dx.adapter.vendor.dto.VendorPanVerificationResponse;
import org.egov.dx.adapter.vendor.pan.PanGatewayAdapter;
import org.egov.dx.util.Configurations;
import org.egov.dx.util.PanConstants;
import org.egov.dx.util.PanNumberUtils;
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
 * Production implementation of {@link PanGatewayAdapter} connecting to the official
 * Income Tax Department (ITD) e-Filing External Agency Web Service for PAN verification.
 *
 * <p>Strictly satisfies SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles remote HTTP transport and response mapping for live ITD PAN gateway.</li>
 *   <li><b>Liskov Substitution Principle (LSP):</b> Seamlessly substitutes {@link PanGatewayAdapter}.</li>
 * </ul>
 *
 * <p>Requires all properties to be explicitly defined in {@code application.properties} with zero hardcoded fallbacks.</p>
 * <p>Activated when {@code pan.verification.mock.enabled=false}.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "pan.verification.mock.enabled", havingValue = "false")
@Slf4j
public class ItdPanGatewayAdapter implements PanGatewayAdapter {

    private final Configurations configurations;
    private final RestClient restClient;

    /**
     * Constructs the adapter and validates connection settings eagerly.
     *
     * @param configurations application configurations
     */
    @Autowired
    public ItdPanGatewayAdapter(Configurations configurations) {
        this.configurations = configurations;

        // Startup sanity check: verify mandatory HTTP connect timeout configuration
        if (configurations.getPanVerificationConnectTimeout() == null) {
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST, "pan.verification.connect.timeout is not configured");
        }
        // Startup sanity check: verify mandatory HTTP socket read timeout configuration
        if (configurations.getPanVerificationReadTimeout() == null) {
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST, "pan.verification.read.timeout is not configured");
        }
        // Startup sanity check: verify mandatory ITD vendor endpoint base URL
        if (StringUtils.isBlank(configurations.getPanVerificationBaseUrl())) {
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST, "pan.verification.base.url is not configured");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(configurations.getPanVerificationConnectTimeout());
        requestFactory.setReadTimeout(configurations.getPanVerificationReadTimeout());

        this.restClient = RestClient.builder()
                .baseUrl(configurations.getPanVerificationBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Submits outbound PAN verification request to ITD web service and maps vendor response.</p>
     */
    @Override
    public VendorPanVerificationResponse verifyPan(VendorPanVerificationRequest request) {
        String path = configurations.getPanVerificationPath();
        // Endpoint path verification: ensure target URI path is configured
        if (StringUtils.isBlank(path)) {
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST, "pan.verification.path is not configured");
        }
        // Credential validation: ensure client ID is present
        if (StringUtils.isBlank(configurations.getPanVerificationClientId())) {
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST, "pan.verification.client.id is not configured");
        }
        // Credential validation: ensure client secret is present
        if (StringUtils.isBlank(configurations.getPanVerificationClientSecret())) {
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST, "pan.verification.client.secret is not configured");
        }

        String rawPan = request != null && request.getSearchParams() != null
                ? request.getSearchParams().getPan()
                : null;
        String maskedPan = PanNumberUtils.maskPan(rawPan);

        log.info("[LIVE-PAN] Invoking ITD PAN verification endpoint [{}{}] for masked PAN: {}",
                configurations.getPanVerificationBaseUrl(), path, maskedPan);

        try {
            VendorPanVerificationResponse response = restClient.post()
                    .uri(path)
                    .header("Client-Id", configurations.getPanVerificationClientId().trim())
                    .header("Client-Secret", configurations.getPanVerificationClientSecret().trim())
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (clientRequest, clientResponse) -> {
                        log.error("[LIVE-PAN] ITD Gateway returned HTTP error {} for masked PAN {}",
                                clientResponse.getStatusCode(), maskedPan);
                        // Security & PII hygiene (Review comment Medium #2): Only capture full error payload at DEBUG level
                        if (log.isDebugEnabled()) {
                            String responseBody = new String(clientResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            log.debug("[LIVE-PAN] ITD Gateway error payload for masked PAN {}: {}", maskedPan, responseBody);
                        }
                        throw new CustomException(PanConstants.ERR_CODE_GATEWAY_ERROR,
                                "ITD PAN gateway failed with status " + clientResponse.getStatusCode());
                    })
                    .body(VendorPanVerificationResponse.class);

            // Null response guard: fail safely if gateway returns 200 OK with empty body
            if (response == null) {
                log.error("[LIVE-PAN] Empty response body from ITD Gateway for masked PAN: {}", maskedPan);
                throw new CustomException(PanConstants.ERR_CODE_GATEWAY_ERROR, PanConstants.MSG_GATEWAY_ERROR);
            }

            log.info("[LIVE-PAN] ITD Gateway successfully processed masked PAN: {}, status: {}",
                    maskedPan, response.getHeader() != null ? response.getHeader().getStatus() : "null");
            return response;

        } catch (CustomException ce) {
            throw ce;
        } catch (Exception ex) {
            log.error("[LIVE-PAN] Unexpected transport exception for masked PAN {}: {}", maskedPan, ex.getMessage(), ex);
            throw new CustomException(PanConstants.ERR_CODE_GATEWAY_ERROR,
                    "Failed to communicate with ITD PAN gateway: " + ex.getMessage());
        }
    }
}
