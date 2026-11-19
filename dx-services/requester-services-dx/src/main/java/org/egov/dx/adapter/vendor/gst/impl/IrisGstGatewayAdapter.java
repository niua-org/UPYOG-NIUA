package org.egov.dx.adapter.vendor.gst.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.dx.adapter.vendor.dto.VendorGstProfileResponse;
import org.egov.dx.adapter.vendor.gst.GstGatewayAdapter;
import org.egov.dx.util.Configurations;
import org.egov.dx.util.GstConstants;
import org.egov.dx.util.GstNumberUtils;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Production implementation of {@link GstGatewayAdapter} connecting to the official
 * IRIS Peridot / Taxpayer GST Search API suite.
 *
 * <p>Strictly satisfies SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles remote HTTP transport and response mapping for live IRIS GST gateway.</li>
 *   <li><b>Liskov Substitution Principle (LSP):</b> Seamlessly substitutes {@link GstGatewayAdapter}.</li>
 * </ul>
 *
 * <p>Requires all properties to be explicitly defined in {@code application.properties} with zero hardcoded fallbacks.</p>
 * <p>Activated when {@code gst.verification.mock.enabled=false}.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "gst.verification.mock.enabled", havingValue = "false")
@Slf4j
public class IrisGstGatewayAdapter implements GstGatewayAdapter {

    private final Configurations configurations;
    private final RestClient restClient;

    /**
     * Constructs the adapter and validates connection settings eagerly.
     *
     * @param configurations application configurations
     */
    @Autowired
    public IrisGstGatewayAdapter(Configurations configurations) {
        this.configurations = configurations;

        if (configurations.getGstVerificationConnectTimeout() == null) {
            throw new CustomException(GstConstants.ERR_CODE_INVALID_REQUEST, "gst.verification.connect.timeout is not configured");
        }
        if (configurations.getGstVerificationReadTimeout() == null) {
            throw new CustomException(GstConstants.ERR_CODE_INVALID_REQUEST, "gst.verification.read.timeout is not configured");
        }
        if (StringUtils.isBlank(configurations.getGstVerificationBaseUrl())) {
            throw new CustomException(GstConstants.ERR_CODE_INVALID_REQUEST, "gst.verification.base.url is not configured");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(configurations.getGstVerificationConnectTimeout());
        requestFactory.setReadTimeout(configurations.getGstVerificationReadTimeout());

        this.restClient = RestClient.builder()
                .baseUrl(configurations.getGstVerificationBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public VendorGstProfileResponse searchGstin(String gstin) {
        String path = configurations.getGstVerificationPath();
        if (StringUtils.isBlank(path)) {
            throw new CustomException(GstConstants.ERR_CODE_INVALID_REQUEST, "gst.verification.path is not configured");
        }
        if (StringUtils.isBlank(configurations.getGstVerificationApiKey())) {
            throw new CustomException(GstConstants.ERR_CODE_INVALID_REQUEST, "gst.verification.api.key is not configured");
        }

        String normalizedGstin = GstNumberUtils.normalizeGstin(gstin);
        String maskedGstin = GstNumberUtils.maskGstin(normalizedGstin);

        log.info("[LIVE-GST] Invoking IRIS GST taxpayer endpoint [{}{}] for masked GSTIN: {}",
                configurations.getGstVerificationBaseUrl(), path, maskedGstin);

        try {
            VendorGstProfileResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("gstin", normalizedGstin)
                            .build())
                    .header("apikey", configurations.getGstVerificationApiKey().trim())
                    .header("x-api-key", configurations.getGstVerificationApiKey().trim())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (clientRequest, clientResponse) -> {
                        String responseBody = new String(clientResponse.getBody().readAllBytes());
                        log.error("[LIVE-GST] IRIS Gateway returned HTTP error {} for masked GSTIN {}: {}",
                                clientResponse.getStatusCode(), maskedGstin, responseBody);
                        throw new CustomException(GstConstants.ERR_CODE_GATEWAY_ERROR,
                                "IRIS GST gateway failed with status " + clientResponse.getStatusCode());
                    })
                    .body(VendorGstProfileResponse.class);

            if (response == null) {
                log.error("[LIVE-GST] Empty response body from IRIS Gateway for masked GSTIN: {}", maskedGstin);
                throw new CustomException(GstConstants.ERR_CODE_GATEWAY_ERROR, GstConstants.MSG_GATEWAY_ERROR);
            }

            log.info("[LIVE-GST] IRIS Gateway successfully processed masked GSTIN: {}, status: {}",
                    maskedGstin, response.getStatusCode());
            return response;

        } catch (CustomException ce) {
            throw ce;
        } catch (Exception ex) {
            log.error("[LIVE-GST] Unexpected transport exception for masked GSTIN {}: {}", maskedGstin, ex.getMessage(), ex);
            throw new CustomException(GstConstants.ERR_CODE_GATEWAY_ERROR,
                    "Failed to communicate with IRIS GST gateway: " + ex.getMessage());
        }
    }
}
