package org.egov.user.persistence.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.kafka.CustomKafkaTemplate;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.model.SMSRequest;
import org.egov.user.domain.service.utils.NotificationUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.util.MaskUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link PropertyOtpClient} managing OTP dispatch, SMS publishing,
 * and OTP validation via {@code egov-otp} or mock test configurations.
 *
 * <p>Strictly adheres to the <b>Single Responsibility Principle (SRP)</b> by isolating external
 * notification transport logic from property domain orchestration.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Repository
@Slf4j
public class PropertyOtpClientImpl implements PropertyOtpClient {

    private final UserConfiguration userConfiguration;
    private final RestTemplate restTemplate;
    private final NotificationUtil notificationUtil;
    private final CustomKafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Constructs {@link PropertyOtpClientImpl} with required configuration and optional communication templates.
     *
     * @param userConfiguration application configuration
     * @param restTemplate HTTP rest template
     * @param notificationUtil SMS topic utility
     * @param kafkaTemplate Kafka message publisher
     */
    @Autowired
    public PropertyOtpClientImpl(UserConfiguration userConfiguration,
                                 @Autowired(required = false) RestTemplate restTemplate,
                                 @Autowired(required = false) NotificationUtil notificationUtil,
                                 @Autowired(required = false) CustomKafkaTemplate<String, Object> kafkaTemplate) {
        this.userConfiguration = userConfiguration;
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
        this.notificationUtil = notificationUtil;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void sendOtp(String mobileNumber, String tenantId, RequestInfo requestInfo) {
        boolean isFixedOtp = Boolean.TRUE.equals(userConfiguration.getPropertyOtpFixedEnabled());

        if (isFixedOtp) {
            log.info("[PROPERTY-OTP-CLIENT] Fixed/mock OTP mode active for mobile: {}", MaskUtils.maskMobile(mobileNumber));
            return;
        }

        String otpHost = userConfiguration.getOtpServiceHost();
        String otpCreateEndpoint = userConfiguration.getOtpCreateEndpoint();

        if (StringUtils.isBlank(otpHost) || StringUtils.isBlank(otpCreateEndpoint)) {
            log.error("[PROPERTY-OTP-CLIENT] egov-otp service host or create endpoint is not configured.");
            throw new CustomException(UserConstants.ERROR_OTP_SERVICE_CONFIG_MISSING, UserConstants.MSG_OTP_SERVICE_CONFIG_MISSING);
        }

        String url = otpHost.endsWith("/")
                ? otpHost + (otpCreateEndpoint.startsWith("/") ? otpCreateEndpoint.substring(1) : otpCreateEndpoint)
                : otpHost + (otpCreateEndpoint.startsWith("/") ? otpCreateEndpoint : "/" + otpCreateEndpoint);

        Map<String, Object> innerOtp = new HashMap<>();
        innerOtp.put(UserConstants.OTP_JSON_IDENTITY, mobileNumber);
        innerOtp.put(UserConstants.OTP_JSON_TENANT_ID, tenantId);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(UserConstants.OTP_JSON_OTP, innerOtp);
        if (requestInfo != null) {
            requestBody.put(UserConstants.OTP_JSON_REQUEST_INFO, requestInfo);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response == null || !response.containsKey(UserConstants.OTP_JSON_OTP)) {
                log.error("[PROPERTY-OTP-CLIENT] OTP service returned null or missing OTP response object at {}", url);
                throw new CustomException(UserConstants.ERROR_OTP_DISPATCH_FAILED, UserConstants.MSG_OTP_DISPATCH_FAILED);
            }
            Object otpObj = response.get(UserConstants.OTP_JSON_OTP);
            if (otpObj instanceof Map) {
                Map<String, Object> otpMap = (Map<String, Object>) otpObj;
                String otpVal = otpMap.get(UserConstants.OTP_JSON_OTP) != null ? String.valueOf(otpMap.get(UserConstants.OTP_JSON_OTP)) : null;

                if (notificationUtil != null && StringUtils.isNotBlank(notificationUtil.smsNotificationTopic) && kafkaTemplate != null && otpVal != null) {
                    SMSRequest smsRequest = new SMSRequest();
                    smsRequest.setMobileNumber(mobileNumber);
                    smsRequest.setMessage(UserConstants.OTP_SMS_TEMPLATE_PREFIX + otpVal + UserConstants.OTP_SMS_TEMPLATE_SUFFIX);
                    kafkaTemplate.send(notificationUtil.smsNotificationTopic, smsRequest);
                }
            }
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("[PROPERTY-OTP-CLIENT] Failed to dispatch OTP via egov-otp at {}: {}", url, e.getMessage());
            throw new CustomException(UserConstants.ERROR_OTP_DISPATCH_FAILED, UserConstants.MSG_OTP_DISPATCH_FAILED);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean validateOtp(String mobileNumber, String otp, String tenantId, RequestInfo requestInfo) {
        boolean isFixedOtp = Boolean.TRUE.equals(userConfiguration.getPropertyOtpFixedEnabled());

        if (isFixedOtp) {
            String fixedVal = userConfiguration.getPropertyOtpFixedValue();
            return StringUtils.isNotBlank(fixedVal) && fixedVal.trim().equals(otp.trim());
        }

        String otpHost = userConfiguration.getOtpServiceHost();
        String otpValidateEndpoint = userConfiguration.getOtpValidateEndpoint();

        if (StringUtils.isBlank(otpHost) || StringUtils.isBlank(otpValidateEndpoint)) {
            log.error("[PROPERTY-OTP-CLIENT] egov-otp service host or validate endpoint is not configured.");
            return false;
        }

        String url = otpHost.endsWith("/")
                ? otpHost + (otpValidateEndpoint.startsWith("/") ? otpValidateEndpoint.substring(1) : otpValidateEndpoint)
                : otpHost + (otpValidateEndpoint.startsWith("/") ? otpValidateEndpoint : "/" + otpValidateEndpoint);

        Map<String, Object> innerOtp = new HashMap<>();
        innerOtp.put(UserConstants.OTP_JSON_IDENTITY, mobileNumber);
        innerOtp.put(UserConstants.OTP_JSON_TENANT_ID, tenantId);
        innerOtp.put(UserConstants.OTP_JSON_OTP, otp.trim());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(UserConstants.OTP_JSON_REQUEST_INFO, requestInfo);
        requestBody.put(UserConstants.OTP_JSON_OTP, innerOtp);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey(UserConstants.OTP_JSON_OTP)) {
                Object otpObj = response.get(UserConstants.OTP_JSON_OTP);
                if (otpObj instanceof Map) {
                    Map<String, Object> otpMap = (Map<String, Object>) otpObj;
                    Object validationSuccessful = otpMap.containsKey(UserConstants.OTP_JSON_IS_VALIDATION_SUCCESSFUL)
                            ? otpMap.get(UserConstants.OTP_JSON_IS_VALIDATION_SUCCESSFUL)
                            : otpMap.get(UserConstants.OTP_JSON_VALIDATION_SUCCESSFUL);
                    if (validationSuccessful != null) {
                        return Boolean.parseBoolean(String.valueOf(validationSuccessful));
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("[PROPERTY-OTP-CLIENT] Exception during OTP validation via egov-otp at {}: {}", url, e.getMessage());
            return false;
        }
    }
}
