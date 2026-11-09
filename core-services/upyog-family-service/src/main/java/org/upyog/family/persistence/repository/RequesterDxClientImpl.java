package org.upyog.family.persistence.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.upyog.family.util.MaskUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client implementation communicating with UPYOG requester-services-dx for Aadhaar OTP consent verification.
 *
 * <p>Enforces strict fail-closed security:
 * <ul>
 *   <li>Mock tokens (e.g. CONSENT-VERIFIED-, MOCK-OTP-) are strictly rejected unless family.consent.mock.enabled=true.</li>
 *   <li>Calls requester-services-dx /_verifyOtp using referenceId and OTP contract.</li>
 *   <li>Binds the token to the specific member and family aggregate to prevent replay attacks.</li>
 *   <li>Verifies the returned gateway masked Aadhaar against the member's Aadhaar reference.</li>
 *   <li>Never allows fallbacks on HTTP or serialization exceptions.</li>
 * </ul>
 * </p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Repository
@Slf4j
public class RequesterDxClientImpl implements RequesterDxClient {

    private final RestTemplate restTemplate;
    private final FamilyConfiguration configuration;
    private final ObjectMapper objectMapper;

    /**
     * In-memory replay tracking cache binding consumed consent tokens to citizen and family identities.
     */
    private final Map<String, TokenBinding> consumedTokens = new ConcurrentHashMap<>();

    /**
     * In-memory cache of verified HoF consent tokens issued through family OTP verification.
     */
    private final Map<String, TokenBinding> registeredFamilyConsentTokens = new ConcurrentHashMap<>();

    private static class TokenBinding {
        private final String memberId;
        private final String familyId;
        private final long timestamp;

        TokenBinding(String memberId, String familyId, long timestamp) {
            this.memberId = memberId;
            this.familyId = familyId;
            this.timestamp = timestamp;
        }
    }

    /**
     * Constructs a RequesterDxClientImpl with RestTemplate, FamilyConfiguration, and ObjectMapper.
     *
     * @param restTemplate Spring RestTemplate for making HTTP calls to requester-services-dx
     * @param configuration family module configuration for endpoints and timeouts
     * @param objectMapper Jackson ObjectMapper for parsing response payloads
     */
    @Autowired
    public RequesterDxClientImpl(RestTemplate restTemplate,
                                 FamilyConfiguration configuration,
                                 ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.configuration = configuration;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Registers a verified Head of Family consent token bound to a target family.
     *
     * @param consentToken the verified consent token string
     * @param familyId target family identifier
     */
    @Override
    public void registerConsentToken(String consentToken, String familyId) {
        registerConsentToken(consentToken, familyId, null);
    }

    /**
     * Registers a verified Head of Family consent token bound to both a target family and an applicant UUID.
     *
     * @param consentToken the verified consent token string
     * @param familyId target family identifier
     * @param applicantUuid citizen user UUID permitted to use this token
     */
    @Override
    public void registerConsentToken(String consentToken, String familyId, String applicantUuid) {
        if (StringUtils.isNotBlank(consentToken)) {
            registeredFamilyConsentTokens.put(consentToken.trim(),
                    new TokenBinding(applicantUuid, familyId, System.currentTimeMillis()));
            log.info("[UPYOG-DX-CLIENT] Registered verified HoF consent token '{}' for family '{}' and applicant '{}'",
                    MaskUtils.maskToken(consentToken), familyId, applicantUuid);
        }
    }

    /**
     * Backward-compatible 3-arg overload forwarding to 5-arg implementation.
     */
    @Override
    public boolean verifyConsentToken(String consentToken, String tenantId, RequestInfo requestInfo) {
        return verifyConsentToken(consentToken, null, null, tenantId, requestInfo);
    }

    /**
     * Verifies an Aadhaar OTP verification consent token with UPYOG requester-services-dx,
     * ensuring strict binding to member identity and family aggregate.
     *
     * @param consentToken        verification token, OTP string, or referenceId:otp payload
     * @param memberUuidOrAadhaar member UUID or Aadhaar reference
     * @param familyId            target family identifier
     * @param tenantId            tenant identifier
     * @param requestInfo         UPYOG request context envelope
     * @return true if consent is valid and verified; false otherwise
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean verifyConsentToken(String consentToken,
                                      String memberUuidOrAadhaar,
                                      String familyId,
                                      String tenantId,
                                      RequestInfo requestInfo) {
        if (StringUtils.isBlank(consentToken)) {
            log.warn("[UPYOG-DX-CLIENT] Consent token is blank. Consent validation failed.");
            return false;
        }

        String trimmedToken = consentToken.trim();

        // 0. Registered HoF consent token check: Issued during verified family OTP workflow
        TokenBinding registeredBinding = registeredFamilyConsentTokens.get(trimmedToken);
        if (registeredBinding != null) {
            // Check TTL expiration (configurable via family.consent.token.ttl.minutes, defaults to 15 minutes)
            if (configuration == null) {
                throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "FamilyConfiguration is required");
            }
            long ttlMs = configuration.getConsentTokenTtlMs();
            long elapsed = System.currentTimeMillis() - registeredBinding.timestamp;
            if (elapsed > ttlMs) {
                registeredFamilyConsentTokens.remove(trimmedToken);
                log.warn("[UPYOG-DX-CLIENT] Registered consent token expired after {} ms (TTL limit: {} ms) for family '{}'",
                        elapsed, ttlMs, familyId);
                return false;
            }

            if (StringUtils.isNotBlank(familyId) && StringUtils.isNotBlank(registeredBinding.familyId)
                    && !registeredBinding.familyId.equals(familyId)) {
                log.warn("[UPYOG-DX-CLIENT] Registered consent token family mismatch: token bound to '{}', requested family '{}'",
                        registeredBinding.familyId, familyId);
                return false;
            }

            if (StringUtils.isNotBlank(registeredBinding.memberId) && StringUtils.isNotBlank(memberUuidOrAadhaar)
                    && !registeredBinding.memberId.equals(memberUuidOrAadhaar)) {
                log.warn("[UPYOG-DX-CLIENT] Registered consent token applicant mismatch: token bound to applicant '{}', requested member '{}'",
                        MaskUtils.maskIdentifier(registeredBinding.memberId), MaskUtils.maskIdentifier(memberUuidOrAadhaar));
                return false;
            }

            // Evict single-use token to prevent replay attacks
            registeredFamilyConsentTokens.remove(trimmedToken);

            log.info("[UPYOG-DX-CLIENT] Token '{}' successfully verified and consumed for family '{}'",
                    MaskUtils.maskToken(trimmedToken), familyId);
            recordTokenBinding(trimmedToken, memberUuidOrAadhaar, familyId);
            return true;
        }

        // 1. Mock Prefix Handling: Strictly gated behind family.consent.mock.enabled flag
        if (trimmedToken.startsWith("CONSENT-VERIFIED-") || trimmedToken.startsWith("MOCK-OTP-")) {
            if (Boolean.TRUE.equals(configuration.getMockConsentEnabled())) {
                log.info("[UPYOG-DX-CLIENT] Mock consent mode enabled (family.consent.mock.enabled=true). "
                        + "Accepting mock consent token: '{}' for member: '{}', family: '{}'",
                        MaskUtils.maskToken(trimmedToken), MaskUtils.maskAadhaar(memberUuidOrAadhaar), familyId);
                recordTokenBinding(trimmedToken, memberUuidOrAadhaar, familyId);
                return true;
            } else {
                log.warn("[UPYOG-DX-CLIENT] Security Alert: Mock consent token '{}' rejected. "
                        + "Mock consent is disabled (family.consent.mock.enabled=false).", MaskUtils.maskToken(trimmedToken));
                return false;
            }
        }

        // 2. Parse Reference ID and OTP from consentToken payload
        String referenceId = null;
        String otp = null;

        if (trimmedToken.contains(":")) {
            String[] parts = trimmedToken.split(":", 2);
            referenceId = parts[0].trim();
            otp = parts[1].trim();
        } else if (trimmedToken.startsWith("{") && trimmedToken.endsWith("}")) {
            try {
                JsonNode root = objectMapper.readTree(trimmedToken);
                if (root.has("referenceId")) {
                    referenceId = root.get("referenceId").asText();
                } else if (root.has("txnId")) {
                    referenceId = root.get("txnId").asText();
                }
                if (root.has("otp")) {
                    otp = root.get("otp").asText();
                }
            } catch (Exception e) {
                log.warn("[UPYOG-DX-CLIENT] Failed to parse JSON consent token payload: {}", e.getMessage());
            }
        }

        if (StringUtils.isBlank(referenceId)) {
            referenceId = trimmedToken;
        }

        // 3. Replay Prevention: Check if token has already been consumed
        String tokenKey = StringUtils.isNotBlank(referenceId) ? referenceId : trimmedToken;
        TokenBinding existingBinding = consumedTokens.get(tokenKey);
        if (existingBinding != null) {
            log.warn("[UPYOG-DX-CLIENT] Replay attack prevented! Token '{}' was previously consumed for member: '{}', family: '{}'.",
                    MaskUtils.maskToken(tokenKey), MaskUtils.maskAadhaar(existingBinding.memberId), existingBinding.familyId);
            return false;
        }

        if (configuration == null || StringUtils.isBlank(configuration.getRequesterDxHost())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "requester.services.dx.host is not configured");
        }
        if (StringUtils.isBlank(configuration.getRequesterDxVerifyOtpPath())) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, "requester.services.dx.verify.otp.path is not configured");
        }
        String url = configuration.getRequesterDxHost() + configuration.getRequesterDxVerifyOtpPath();
        log.info("[UPYOG-DX-CLIENT] Calling requester-services-dx at: {} for referenceId: {}", url, MaskUtils.maskIdentifier(referenceId));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("RequestInfo", requestInfo);
        requestBody.put("referenceId", referenceId);
        if (StringUtils.isNotBlank(otp)) {
            requestBody.put("otp", otp);
        }
        if (StringUtils.isNotBlank(memberUuidOrAadhaar)) {
            requestBody.put("userUuid", memberUuidOrAadhaar);
            if (memberUuidOrAadhaar.matches("^[2-9]{1}[0-9]{11}$")) {
                requestBody.put("aadhaarNumber", memberUuidOrAadhaar);
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response == null || !response.containsKey("isVerified")) {
                log.warn("[UPYOG-DX-CLIENT] requester-services-dx returned invalid or empty verification response for referenceId: {}",
                        MaskUtils.maskIdentifier(referenceId));
                return false;
            }

            Object isVerifiedObj = response.get("isVerified");
            boolean isVerified = Boolean.TRUE.equals(isVerifiedObj) || "true".equalsIgnoreCase(String.valueOf(isVerifiedObj));
            if (!isVerified) {
                log.warn("[UPYOG-DX-CLIENT] requester-services-dx reported isVerified=false for referenceId: {}",
                        MaskUtils.maskIdentifier(referenceId));
                return false;
            }

            // 5. Verify the token against member's Aadhaar reference if present
            Object maskedAadhaarObj = response.get("maskedAadhaar");
            String responseMaskedAadhaar = maskedAadhaarObj != null ? String.valueOf(maskedAadhaarObj) : null;
            if (StringUtils.isNotBlank(memberUuidOrAadhaar) && StringUtils.isNotBlank(responseMaskedAadhaar)) {
                if (memberUuidOrAadhaar.matches("^[2-9]{1}[0-9]{11}$")) {
                    String expectedLast4 = memberUuidOrAadhaar.substring(8);
                    if (!responseMaskedAadhaar.endsWith(expectedLast4)) {
                        log.error("[UPYOG-DX-CLIENT] Aadhaar mismatch: Gateway returned masked Aadhaar '{}', but member Aadhaar ends with '{}'",
                                responseMaskedAadhaar, expectedLast4);
                        return false;
                    }
                } else if (memberUuidOrAadhaar.matches("^[0-9]{4}$")) {
                    if (!responseMaskedAadhaar.endsWith(memberUuidOrAadhaar)) {
                        log.error("[UPYOG-DX-CLIENT] Aadhaar 4-digit suffix mismatch: Gateway returned '{}', expected '{}'",
                                responseMaskedAadhaar, memberUuidOrAadhaar);
                        return false;
                    }
                }
            }

            // 6. Record successful consumption to prevent future replay
            recordTokenBinding(tokenKey, memberUuidOrAadhaar, familyId);
            log.info("[UPYOG-DX-CLIENT] Successfully validated consent token for referenceId: {}, member: {}, family: {}",
                    MaskUtils.maskIdentifier(referenceId), MaskUtils.maskAadhaar(memberUuidOrAadhaar), familyId);
            return true;

        } catch (Exception e) {
            log.error("[UPYOG-DX-CLIENT] requester-services-dx verification call returned error: {}. Consent verification failed (fail-closed).",
                    e.getMessage(), e);
            return false;
        }
    }

    /**
     * Records token consumption binding to member and family to prevent replay attacks.
     *
     * @param tokenKey unique token cache key
     * @param memberId member UUID or Aadhaar reference
     * @param familyId target family identifier
     */
    private void recordTokenBinding(String tokenKey, String memberId, String familyId) {
        if (StringUtils.isNotBlank(tokenKey)) {
            consumedTokens.put(tokenKey, new TokenBinding(memberId, familyId, System.currentTimeMillis()));
        }
    }
}
