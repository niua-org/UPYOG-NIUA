package org.upyog.family.persistence.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.util.KafkaProducer;
import org.upyog.family.util.MaskUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of OtpClient connecting to UPYOG egov-otp service for OTP creation and validation.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class OtpClientImpl implements OtpClient {

    private final RestTemplate restTemplate;
    private final FamilyConfiguration configuration;
    private final KafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;

    @Override
    @SuppressWarnings("unchecked")
    public String sendOtp(String mobileNumber, String tenantId, RequestInfo requestInfo) {
        log.info("[UPYOG-OTP-CLIENT] Initiating OTP dispatch to mobile: {}, tenant: {}", MaskUtils.maskMobile(mobileNumber), tenantId);

        String otpHost = configuration.getOtpHost();
        String otpCreatePath = configuration.getOtpCreatePath();
        String url = otpHost + otpCreatePath;

        Map<String, Object> innerOtp = new HashMap<>();
        innerOtp.put("identity", mobileNumber);
        innerOtp.put("tenantId", tenantId);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("otp", innerOtp);
        if (requestInfo != null) {
            requestBody.put("RequestInfo", requestInfo);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey("otp")) {
                Object otpObj = response.get("otp");
                if (otpObj instanceof Map) {
                    Map<String, Object> otpMap = (Map<String, Object>) otpObj;
                    String uuid = (String) (otpMap.containsKey("UUID") ? otpMap.get("UUID") : otpMap.get("uuid"));
                    String otpVal = otpMap.get("otp") != null ? String.valueOf(otpMap.get("otp")) : null;

                    // If OTP value is blank, gateway failed to create OTP - fail fast rather than reporting false success
                    if (StringUtils.isBlank(otpVal)) {
                        log.error("[UPYOG-OTP-CLIENT] Blank OTP value returned from egov-otp for reference UUID: {}", MaskUtils.maskIdentifier(uuid));
                        throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                                "Failed to generate valid OTP code from OTP service.");
                    }

                    // Publish SMS notification using configurable message template
                    if (StringUtils.isNotBlank(configuration.getSmsNotifTopic())) {
                        Map<String, Object> smsPayload = new HashMap<>();
                        smsPayload.put("mobileNumber", mobileNumber);
                        String template = StringUtils.isNotBlank(configuration.getSmsOtpMessageTemplate())
                                ? configuration.getSmsOtpMessageTemplate()
                                : "Your OTP to authorize joining your family in UPYOG is %s.";
                        smsPayload.put("message", String.format(template, otpVal));
                        smsPayload.put("category", "OTP");
                        kafkaProducer.push(configuration.getSmsNotifTopic(), smsPayload);
                    }

                    if (StringUtils.isNotBlank(uuid)) {
                        log.info("[UPYOG-OTP-CLIENT] Successfully generated OTP with reference UUID: {}", MaskUtils.maskIdentifier(uuid));
                        return uuid;
                    }
                }
            }
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            log.warn("[UPYOG-OTP-CLIENT] Error calling egov-otp create endpoint at {}: {}", url, e.getMessage());
            if (!Boolean.TRUE.equals(configuration.getMockConsentEnabled())) {
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST,
                        "Failed to send OTP to mobile: " + e.getMessage());
            }
        }

        if (Boolean.TRUE.equals(configuration.getMockConsentEnabled())) {
            String mockReference = "FAM-OTP-REF-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("[UPYOG-OTP-CLIENT] Mock consent enabled. Generated mock OTP reference: {} for mobile: {}",
                    MaskUtils.maskIdentifier(mockReference), MaskUtils.maskMobile(mobileNumber));
            return mockReference;
        }

        throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "Failed to generate OTP via gateway.");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean validateOtp(String mobileNumber, String otp, String tenantId, RequestInfo requestInfo) {
        log.info("[UPYOG-OTP-CLIENT] Validating OTP for mobile: {}, tenant: {}", MaskUtils.maskMobile(mobileNumber), tenantId);

        if (StringUtils.isBlank(otp)) {
            return false;
        }

        // Check mock consent bypass - strictly gated by mockConsentEnabled=true AND configured mockFixedOtp
        if (Boolean.TRUE.equals(configuration.getMockConsentEnabled())) {
            String configuredFixedOtp = configuration.getMockFixedOtp();
            if (StringUtils.isNotBlank(configuredFixedOtp) && configuredFixedOtp.trim().equals(otp.trim())) {
                log.info("[UPYOG-OTP-CLIENT] Mock consent enabled. Accepted configured fixed OTP for mobile: {}",
                        MaskUtils.maskMobile(mobileNumber));
                return true;
            }
        }

        String otpHost = configuration.getOtpHost();
        String otpValidatePath = configuration.getOtpValidatePath();
        String url = otpHost + otpValidatePath;

        Map<String, Object> innerOtp = new HashMap<>();
        innerOtp.put("identity", mobileNumber);
        innerOtp.put("tenantId", tenantId);
        innerOtp.put("otp", otp.trim());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("RequestInfo", requestInfo);
        requestBody.put("otp", innerOtp);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey("otp")) {
                Object otpObj = response.get("otp");
                if (otpObj instanceof Map) {
                    Map<String, Object> otpMap = (Map<String, Object>) otpObj;
                    Object validationSuccessful = otpMap.containsKey("isValidationSuccessful")
                            ? otpMap.get("isValidationSuccessful")
                            : otpMap.get("validationSuccessful");
                    if (validationSuccessful != null) {
                        return Boolean.parseBoolean(String.valueOf(validationSuccessful));
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("[UPYOG-OTP-CLIENT] Exception during OTP validation at {}: {}", url, e.getMessage());
            return false;
        }
    }
}
