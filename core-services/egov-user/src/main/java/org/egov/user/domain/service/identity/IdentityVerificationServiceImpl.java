package org.egov.user.domain.service.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.enums.Gender;
import org.egov.user.domain.service.utils.UserConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.egov.user.config.UserConfiguration;

/**
 * Implementation of {@link IdentityVerificationService} communicating with identity verification adapter
 * (e.g. requester-services-dx or identity-verification-service) with development fallback.
 */
@Service
@Slf4j
public class IdentityVerificationServiceImpl implements IdentityVerificationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final UserConfiguration userConfiguration;
    private final Map<String, Integer> otpAttemptsByRefId = new ConcurrentHashMap<>();

    @Autowired
    public IdentityVerificationServiceImpl(RestTemplate restTemplate,
                                           ObjectMapper objectMapper,
                                           @Autowired(required = false) UserConfiguration userConfiguration) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userConfiguration = userConfiguration;
    }

    public IdentityVerificationServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this(restTemplate, objectMapper, null);
    }

    private String getRequesterServiceHost() {
        return (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getRequesterServiceHost()))
                ? userConfiguration.getRequesterServiceHost()
                : "https://upyog.niua.org/";
    }

    private String getRequesterVerifyEndpoint() {
        return (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getRequesterAadhaarVerifyEndpoint()))
                ? userConfiguration.getRequesterAadhaarVerifyEndpoint()
                : "requester-services-dx/aadhaar/v1/_verifyOtp";
    }

    private boolean isFixedOTPEnabled() {
        return userConfiguration != null && Boolean.TRUE.equals(userConfiguration.getFixedOtpEnabled());
    }

    private String getFixedOTPPassword() {
        return (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getFixedOtpPassword()))
                ? userConfiguration.getFixedOtpPassword()
                : "123456";
    }

    private int getMaxOtpAttempts() {
        return (userConfiguration != null && userConfiguration.getAadhaarOtpMaxAttempts() != null)
                ? userConfiguration.getAadhaarOtpMaxAttempts()
                : 3;
    }

    @Override
    public VerifiedAadhaarDemographics verifyAadhaarOtp(RequestInfo requestInfo,
                                                        String tenantId,
                                                        String refId,
                                                        String rawAadhaar,
                                                        String otp,
                                                        boolean isResync) {
        log.info("[IDENTITY-VERIFY-SERVICE] Initiating Aadhaar OTP verification. tenant={}, refId={}, isResync={}",
                tenantId, refId, isResync);

        if (StringUtils.isNotBlank(refId)) {
            int currentAttempts = otpAttemptsByRefId.getOrDefault(refId, 0);
            if (currentAttempts >= getMaxOtpAttempts()) {
                log.error("[IDENTITY-VERIFY-SERVICE] Locked transaction: refId {} exceeded max invalid OTP attempts ({})", refId, getMaxOtpAttempts());
                throw new CustomException(UserConstants.ERROR_MAX_OTP_ATTEMPTS_EXCEEDED, UserConstants.MSG_MAX_OTP_ATTEMPTS_EXCEEDED);
            }
        }

        if (isFixedOTPEnabled() && StringUtils.isNotBlank(getFixedOTPPassword()) && getFixedOTPPassword().equals(otp)) {
            log.info("[IDENTITY-VERIFY-SERVICE] Fixed OTP matched. Producing verified dummy demographic payload.");
            if (StringUtils.isNotBlank(refId)) {
                otpAttemptsByRefId.remove(refId);
            }
            return createMockDemographics(rawAadhaar, refId);
        }

        try {
            String host = getRequesterServiceHost();
            String endpoint = getRequesterVerifyEndpoint();
            String url = host.endsWith("/")
                    ? host + (endpoint.startsWith("/") ? endpoint.substring(1) : endpoint)
                    : host + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);

            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("RequestInfo", requestInfo);
            reqBody.put("txnId", refId);
            reqBody.put("referenceId", refId);
            reqBody.put("otp", otp);
            reqBody.put("isResync", isResync);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            log.info("[IDENTITY-VERIFY-SERVICE] Invoking identity verification adapter at URL: {}", url);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                boolean isVerified = root.path("isVerified").asBoolean(false);
                if (!isVerified) {
                    recordFailedOtpAttempt(refId);
                    log.error("[IDENTITY-VERIFY-SERVICE] Identity provider reported verification unsuccessful");
                    throw new CustomException(UserConstants.ERROR_AADHAAR_VERIFICATION_FAILED, UserConstants.MSG_AADHAAR_VERIFICATION_REJECTED);
                }

                if (StringUtils.isNotBlank(refId)) {
                    otpAttemptsByRefId.remove(refId);
                }

                String maskedAadhaar = root.path("maskedAadhaar").asText(maskAadhaar(rawAadhaar));
                JsonNode demoNode = root.path(UserConstants.JSON_KEY_DEMOGRAPHICS);
                String name = demoNode.hasNonNull(UserConstants.JSON_KEY_NAME) ? demoNode.path(UserConstants.JSON_KEY_NAME).asText() : root.path(UserConstants.JSON_KEY_NAME).asText(null);
                String genderStr = demoNode.hasNonNull(UserConstants.JSON_KEY_GENDER) ? demoNode.path(UserConstants.JSON_KEY_GENDER).asText() : root.path(UserConstants.JSON_KEY_GENDER).asText(null);
                String dobStr = demoNode.hasNonNull(UserConstants.JSON_KEY_DOB) ? demoNode.path(UserConstants.JSON_KEY_DOB).asText() : root.path(UserConstants.JSON_KEY_DOB).asText(null);
                String photo = demoNode.hasNonNull(UserConstants.JSON_KEY_PHOTO) ? demoNode.path(UserConstants.JSON_KEY_PHOTO).asText() : root.path(UserConstants.JSON_KEY_PHOTO).asText(null);

                log.info("[IDENTITY-VERIFY-SERVICE] e-KYC demographics verified successfully. maskedAadhaar={}", maskedAadhaar);
                return VerifiedAadhaarDemographics.builder()
                        .maskedAadhaar(maskedAadhaar)
                        .name(name)
                        .gender(parseGender(genderStr))
                        .dob(parseDob(dobStr))
                        .photo(photo)
                        .verified(true)
                        .build();
            }
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            recordFailedOtpAttempt(refId);
            log.error("[IDENTITY-VERIFY-SERVICE] External identity provider call failed ({}: {}). Failing closed.",
                    e.getClass().getSimpleName(), e.getMessage(), e);
            throw new CustomException(UserConstants.ERROR_AADHAAR_VERIFICATION_FAILED, UserConstants.MSG_AADHAAR_VERIFICATION_REJECTED);
        }

        recordFailedOtpAttempt(refId);
        throw new CustomException(UserConstants.ERROR_AADHAAR_VERIFY_ERROR, UserConstants.MSG_AADHAAR_VERIFY_PROVIDER_ERROR);
    }

    /**
     * Records failed OTP verification attempts per reference ID.
     * When attempts reach {@code maxOtpAttempts}, locks the session.
     *
     * @param refId the transaction reference ID
     * @throws CustomException if max attempts are exceeded
     */
    private void recordFailedOtpAttempt(String refId) {
        if (StringUtils.isNotBlank(refId)) {
            int attempts = otpAttemptsByRefId.compute(refId, (k, v) -> (v == null ? 1 : v + 1));
            if (attempts >= getMaxOtpAttempts()) {
                log.error("[IDENTITY-VERIFY-SERVICE] refId {} reached max invalid OTP attempts ({}). Invaliding and locking transaction.",
                        refId, getMaxOtpAttempts());
                throw new CustomException(UserConstants.ERROR_MAX_OTP_ATTEMPTS_EXCEEDED, UserConstants.MSG_MAX_OTP_ATTEMPTS_EXCEEDED);
            }
        }
    }

    /**
     * Creates mock demographic attributes for development or test environments.
     *
     * @param rawAadhaar plain Aadhaar number
     * @param refId      session reference ID
     * @return mock {@link VerifiedAadhaarDemographics}
     */
    private VerifiedAadhaarDemographics createMockDemographics(String rawAadhaar, String refId) {
        String masked = maskAadhaar(rawAadhaar);
        if (StringUtils.isBlank(masked)) {
            masked = "XXXXXXXX" + (StringUtils.isNotBlank(refId) && refId.length() >= 4
                    ? refId.substring(refId.length() - 4) : "1234");
        }
        return VerifiedAadhaarDemographics.builder()
                .maskedAadhaar(masked)
                .name("Aadhaar Verified Citizen")
                .gender(Gender.MALE)
                .dob(parseDob("1990-01-01"))
                .verified(true)
                .build();
    }

    /**
     * Masks Aadhaar number showing only last 4 digits (e.g. XXXXXXXX1234).
     *
     * @param aadhaar raw Aadhaar number
     * @return masked Aadhaar string
     */
    private String maskAadhaar(String aadhaar) {
        if (StringUtils.isBlank(aadhaar)) {
            return null;
        }
        String clean = aadhaar.trim().replaceAll("\\s+", "");
        if (clean.length() == 12) {
            return "XXXXXXXX" + clean.substring(8);
        }
        return "XXXXXXXX" + (clean.length() >= 4 ? clean.substring(clean.length() - 4) : "****");
    }

    /**
     * Parses gender string representation into domain {@link Gender} enum.
     *
     * @param g gender string
     * @return resolved {@link Gender}
     */
    private Gender parseGender(String g) {
        if (StringUtils.isBlank(g)) return null;
        String val = g.trim().toUpperCase();
        if ("MALE".equals(val) || "M".equals(val)) return Gender.MALE;
        if ("FEMALE".equals(val) || "F".equals(val)) return Gender.FEMALE;
        if ("TRANSGENDER".equals(val) || "T".equals(val)) return Gender.TRANSGENDER;
        return Gender.OTHERS;
    }

    /**
     * Parses date of birth string across standard date formats.
     *
     * @param dob date of birth string
     * @return parsed {@link Date} or null if invalid
     */
    private Date parseDob(String dob) {
        if (StringUtils.isBlank(dob)) return null;
        String[] patterns = {"yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy"};
        for (String p : patterns) {
            try {
                return new SimpleDateFormat(p).parse(dob.trim());
            } catch (ParseException ignored) {
                log.trace("[IDENTITY-VERIFY-SERVICE] DOB parse attempt with pattern {} failed for value: {}", p, dob);
            }
        }
        return null;
    }
}
