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
        log.info("[UPYOG-OTP-CLIENT] Initiating OTP dispatch to mobile: {}, tenant: {}", mobileNumber, tenantId);

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

                    // Publish SMS notification if OTP generated
                    if (StringUtils.isNotBlank(otpVal) && StringUtils.isNotBlank(configuration.getSmsNotifTopic())) {
                        Map<String, Object> smsPayload = new HashMap<>();
                        smsPayload.put("mobileNumber", mobileNumber);
                        smsPayload.put("message", "Your OTP to authorize joining your family in UPYOG is " + otpVal + ". Valid for 5 minutes.");
                        smsPayload.put("category", "OTP");
                        kafkaProducer.push(configuration.getSmsNotifTopic(), smsPayload);
                    }

                    if (StringUtils.isNotBlank(uuid)) {
                        log.info("[UPYOG-OTP-CLIENT] Successfully generated OTP with reference UUID: {}", uuid);
                        return uuid;
                    }
                }
            }
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
                    mockReference, mobileNumber);
            return mockReference;
        }

        throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "Failed to generate OTP via gateway.");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean validateOtp(String mobileNumber, String otp, String tenantId, RequestInfo requestInfo) {
        log.info("[UPYOG-OTP-CLIENT] Validating OTP for mobile: {}, tenant: {}", mobileNumber, tenantId);

        if (StringUtils.isBlank(otp)) {
            return false;
        }

        // Check mock consent bypass
        if (Boolean.TRUE.equals(configuration.getMockConsentEnabled())) {
            if ("123456".equals(otp.trim()) || "1234".equals(otp.trim()) || otp.trim().startsWith("MOCK-")) {
                log.info("[UPYOG-OTP-CLIENT] Mock consent enabled. Accepted mock OTP: {}", otp);
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
            if (Boolean.TRUE.equals(configuration.getMockConsentEnabled()) && "123456".equals(otp.trim())) {
                return true;
            }
            return false;
        }
    }
}
