package org.egov.dx.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.service.TenantVerificationConfigService;
import org.egov.dx.util.Configurations;
import org.egov.dx.util.PanConstants;
import org.egov.dx.util.PanNumberUtils;
import org.egov.dx.web.models.PanVerificationRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Dedicated validator responsible for input sanity, authentication, and format validation of PAN requests.
 *
 * <p>Enforces the <b>Single Responsibility Principle (SRP)</b> by isolating all validation logic
 * away from the controller and service layers.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class PanVerificationValidator {

    private static final Pattern DOB_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4}|\\d{2}-\\d{2}-\\d{4})$");

    private final TenantVerificationConfigService tenantConfigService;
    private final Configurations configurations;

    /**
     * Constructs {@link PanVerificationValidator} with injected {@link TenantVerificationConfigService}
     * and {@link Configurations}.
     *
     * @param tenantConfigService service providing MDMS configuration and master data lookup
     * @param configurations application configuration properties
     */
    @Autowired
    public PanVerificationValidator(TenantVerificationConfigService tenantConfigService, Configurations configurations) {
        this.tenantConfigService = tenantConfigService;
        this.configurations = configurations;
    }

    /**
     * Validates incoming {@link PanVerificationRequest}.
     *
     * @param request the verification request
     * @throws CustomException if request, RequestInfo, authentication, PAN format, fullName, dob, or gender is invalid
     */
    public void validatePanRequest(PanVerificationRequest request) {
        // Guard check: prevent NullPointerException by rejecting null incoming request payload
        if (request == null) {
            log.error("PAN verification validation failed: request is null");
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST, PanConstants.MSG_INVALID_REQUEST);
        }

        // eGov contract guard: RequestInfo is required for audit trails, tracing, and multi-tenancy
        if (request.getRequestInfo() == null) {
            log.error("PAN verification validation failed: RequestInfo is null");
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST, PanConstants.MSG_INVALID_REQUEST);
        }

        // Security check (Review comment High #1): Mandate authenticated session with valid user UUID
        // to prevent unauthenticated scraping or brute-forcing of citizen PAN verification records
        if (request.getRequestInfo().getUserInfo() == null
                || StringUtils.isBlank(request.getRequestInfo().getUserInfo().getUuid())) {
            log.warn("PAN verification validation failed: unauthenticated request missing userInfo");
            throw new CustomException(PanConstants.ERR_CODE_UNAUTHORIZED_USER, PanConstants.MSG_UNAUTHORIZED_USER);
        }

        String pan = request.getPan();
        // Input sanitization: ensure PAN value is provided and not empty whitespace
        if (StringUtils.isBlank(pan)) {
            log.warn("PAN verification validation failed: PAN is blank");
            throw new CustomException(PanConstants.ERR_CODE_INVALID_FORMAT, PanConstants.MSG_BLANK_PAN);
        }

        // Structural syntax validation: verify PAN conforms to Indian ITD 10-char format ([A-Z]{5}[0-9]{4}[A-Z])
        // to reject malformed inputs before invoking downstream verification adapters or billable vendor APIs
        if (!PanNumberUtils.isValidPan(pan)) {
            log.warn("PAN verification validation failed for invalid format PAN: {}", PanNumberUtils.maskPan(pan));
            throw new CustomException(PanConstants.ERR_CODE_INVALID_FORMAT, PanConstants.MSG_INVALID_FORMAT);
        }

        String maskedPan = PanNumberUtils.maskPan(pan);
        validateFullName(request.getFullName(), maskedPan);
        validateDob(request.getDob(), maskedPan);

        // Multi-tenancy guard: tenantId is required to query MDMS for state/ULB enablement rules and master data
        if (StringUtils.isBlank(request.getTenantId())) {
            log.warn("PAN verification validation failed: tenantId is blank");
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST, "Tenant ID is mandatory for PAN verification.");
        }

        validateGender(request.getGender(), request.getRequestInfo(), request.getTenantId());
    }

    private void validateFullName(String fullName, String maskedPan) {
        // Name validation: fullName is required by ITD gateway to verify citizen identity match
        if (StringUtils.isBlank(fullName)) {
            log.warn("PAN verification validation failed: fullName is blank for masked PAN: {}", maskedPan);
            throw new CustomException(PanConstants.ERR_CODE_MISSING_NAME, PanConstants.MSG_MISSING_NAME);
        }
        String trimmed = fullName.trim();
        // Length bounds: reject names shorter than 2 characters (e.g. initial only) or abnormally long (>150 chars)
        if (trimmed.length() < 2 || trimmed.length() > 150) {
            log.warn("PAN verification validation failed: fullName length out of bounds [{}] for masked PAN: {}",
                    trimmed.length(), maskedPan);
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST,
                    "Full name must be between 2 and 150 characters.");
        }
    }

    private void validateDob(String dob, String maskedPan) {
        // Date of birth is a required matching criterion under Income Tax Department rules
        if (StringUtils.isBlank(dob)) {
            log.warn("PAN verification validation failed: dob is blank for masked PAN: {}", maskedPan);
            throw new CustomException(PanConstants.ERR_CODE_MISSING_DOB, PanConstants.MSG_MISSING_DOB);
        }
        // Format check: ensure date conforms to supported ISO (YYYY-MM-DD) or Indian standard (DD/MM/YYYY)
        if (!DOB_PATTERN.matcher(dob.trim()).matches()) {
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST,
                    "Invalid DOB format. Expected YYYY-MM-DD or DD/MM/YYYY.");
        }
        LocalDate date;
        try {
            String norm = dob.trim().replace('/', '-');
            String[] parts = norm.split("-");
            // Check if year is first (YYYY-MM-DD) or last (DD-MM-YYYY)
            if (parts[0].length() == 4) {
                date = LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            } else {
                date = LocalDate.of(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
            }
        } catch (Exception e) {
            // Calendar sanity: catches non-existent dates like 31-02-2020 or invalid leap day combinations
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST, "Invalid calendar date provided for DOB: " + dob);
        }
        ZoneId zoneId = (configurations != null && StringUtils.isNotBlank(configurations.getAppTimezone()))
                ? ZoneId.of(configurations.getAppTimezone().trim())
                : ZoneId.systemDefault();
        // Logical bounds: citizen cannot have a birth date in the future
        if (date.isAfter(LocalDate.now(zoneId))) {
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST, "DOB cannot be in the future.");
        }
        // Realistic age bound: reject dates older than 125 years to catch typo years (e.g., year 0024 instead of 2024)
        if (date.isBefore(LocalDate.now(zoneId).minusYears(125))) {
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST, "DOB indicates invalid age (> 125 years).");
        }
    }

    private void validateGender(String gender, RequestInfo requestInfo, String tenantId) {
        // Optional field check: if gender is provided, validate against accepted MDMS enumeration
        if (StringUtils.isNotBlank(gender)) {
            Set<String> validGenders = tenantConfigService != null
                    ? tenantConfigService.getValidGenders(requestInfo, tenantId)
                    : Collections.emptySet();
            String normalizedGender = gender.trim().toUpperCase();
            // Enumeration validation: reject gender values not configured in MDMS master data
            if (validGenders == null || !validGenders.contains(normalizedGender)) {
                log.warn("PAN verification validation failed: invalid gender '{}' against MDMS valid genders: {}",
                        gender, validGenders);
                throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST,
                        "Invalid gender '" + gender + "'. Allowed values from MDMS: " + (validGenders != null ? validGenders : "[]"));
            }
        }
    }
}
