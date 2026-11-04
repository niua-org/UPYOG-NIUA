package org.egov.dx.adapter.vendor.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.dx.adapter.vendor.AadhaarGatewayAdapter;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorSendOtpResponse;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpFailureResponse;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpRequest;
import org.egov.dx.adapter.vendor.dto.VendorVerifyOtpSuccessResponse;
import org.egov.dx.util.AadhaarConstants;
import org.egov.dx.util.Configurations;
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
 * Production implementation of {@link AadhaarGatewayAdapter} integrating with
 * Sandbox India Aadhaar OKYC API using Spring 6 / Boot 3 {@link RestClient}.
 *
 * <p>Enforces the <b>Single Responsibility Principle (SRP)</b>: This class is solely responsible for
 * executing live network HTTP requests and mapping vendor-specific responses.</p>
 * <p>Enforces the <b>Liskov Substitution Principle (LSP)</b>: Fully honors the {@link AadhaarGatewayAdapter} contract.</p>
 *
 * <p>Automatically activated by Spring when {@code aadhaar.gateway.mock.enabled=false}.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "aadhaar.gateway.mock.enabled", havingValue = "false")
@Slf4j
public class SandboxAadhaarGatewayAdapter implements AadhaarGatewayAdapter {

    private final Configurations configurations;
    private final RestClient restClient;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Constructs the adapter and configures the underlying synchronous {@link RestClient}
     * with resilient connection and read timeouts.
     *
     * @param configurations application configurations
     */
    public SandboxAadhaarGatewayAdapter(Configurations configurations) {
        this.configurations = configurations;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int connectTimeout = (configurations.getAadhaarGatewayConnectTimeout() != null)
                ? configurations.getAadhaarGatewayConnectTimeout() : 5000;
        int readTimeout = (configurations.getAadhaarGatewayReadTimeout() != null)
                ? configurations.getAadhaarGatewayReadTimeout() : 10000;
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .baseUrl(configurations.getAadhaarGatewayBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public VendorSendOtpResponse sendOtp(VendorSendOtpRequest request) {
        String endpointUri = configurations.getAadhaarGatewaySendOtpPath();
        String maskedAadhaar = (request != null) ? request.getMaskedAadhaar() : "null";

        log.info("[LIVE] Invoking Sandbox Send OTP endpoint [{}{}] for masked Aadhaar: {}",
                configurations.getAadhaarGatewayBaseUrl(), endpointUri, maskedAadhaar);

        try {
            return restClient.post()
                    .uri(endpointUri)
                    .header(AadhaarConstants.HEADER_API_KEY, defaultString(configurations.getAadhaarGatewayApiKey()))
                    .header(AadhaarConstants.HEADER_API_SECRET, defaultString(configurations.getAadhaarGatewayApiSecret()))
                    .header(AadhaarConstants.HEADER_API_VERSION, defaultString(configurations.getAadhaarGatewayApiVersion()))
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.error("[LIVE] Sandbox Send OTP rejected with client error HTTP {}: {}", res.getStatusCode(), res.getStatusText());
                        throw new CustomException(AadhaarConstants.ERR_CODE_GATEWAY_ERROR,
                                "Vendor gateway rejected Send OTP request: " + res.getStatusText());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("[LIVE] Sandbox Send OTP failed with server error HTTP {}: {}", res.getStatusCode(), res.getStatusText());
                        throw new CustomException(AadhaarConstants.ERR_CODE_GATEWAY_ERROR,
                                "Aadhaar vendor gateway service unavailable: " + res.getStatusText());
                    })
                    .body(VendorSendOtpResponse.class);
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception ex) {
            log.error("[LIVE] Sandbox Send OTP API invocation failed: {}", ex.getMessage(), ex);
            throw new CustomException(AadhaarConstants.ERR_CODE_GATEWAY_ERROR,
                    "Failed to communicate with Aadhaar gateway: " + ex.getMessage());
        }
    }

    @Override
    public VendorVerifyOtpSuccessResponse verifyOtp(VendorVerifyOtpRequest request) {
        String endpointUri = configurations.getAadhaarGatewayVerifyOtpPath();
        Long refId = (request != null) ? request.getReferenceId() : null;

        log.info("[LIVE] Invoking Sandbox Verify OTP endpoint [{}{}] for referenceId: {}",
                configurations.getAadhaarGatewayBaseUrl(), endpointUri, refId);

        try {
            return restClient.post()
                    .uri(endpointUri)
                    .header(AadhaarConstants.HEADER_API_KEY, defaultString(configurations.getAadhaarGatewayApiKey()))
                    .header(AadhaarConstants.HEADER_API_SECRET, defaultString(configurations.getAadhaarGatewayApiSecret()))
                    .header(AadhaarConstants.HEADER_API_VERSION, defaultString(configurations.getAadhaarGatewayApiVersion()))
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.warn("[LIVE] Sandbox Verify OTP rejected with client error HTTP {}: {}", res.getStatusCode(), res.getStatusText());
                        String failureMsg = AadhaarConstants.MSG_INVALID_OR_EXPIRED_OTP;
                        try {
                            if (objectMapper != null && res.getBody() != null) {
                                VendorVerifyOtpFailureResponse failureRes = objectMapper.readValue(
                                        res.getBody(), VendorVerifyOtpFailureResponse.class);
                                if (failureRes != null && failureRes.getData() != null
                                        && StringUtils.isNotBlank(failureRes.getData().getMessage())) {
                                    failureMsg = failureRes.getData().getMessage();
                                }
                            }
                        } catch (Exception ignored) {
                            // Fallback to default message
                        }
                        throw new CustomException(AadhaarConstants.STATUS_INVALID_OTP, failureMsg);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("[LIVE] Sandbox Verify OTP failed with server error HTTP {}: {}", res.getStatusCode(), res.getStatusText());
                        throw new CustomException(AadhaarConstants.ERR_CODE_GATEWAY_ERROR,
                                "Aadhaar gateway service unavailable: " + res.getStatusText());
                    })
                    .body(VendorVerifyOtpSuccessResponse.class);
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception ex) {
            log.error("[LIVE] Sandbox Verify OTP API invocation failed: {}", ex.getMessage(), ex);
            throw new CustomException(AadhaarConstants.ERR_CODE_GATEWAY_ERROR,
                    "Failed to communicate with Aadhaar gateway during verification: " + ex.getMessage());
        }
    }

    private String defaultString(String str) {
        return (str != null) ? str : "";
    }
}
