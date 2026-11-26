package org.egov.user.domain.service.asset;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.util.MaskUtils;
import org.egov.user.web.contract.property.UrbanPropertyInfo;
import org.egov.user.web.contract.property.UrbanPropertyOwnerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator enforcing ownership rules, linking eligibility, caller capacity,
 * and resolving destination owner contact for urban property linking workflows.
 *
 * <p>Strictly adheres to the <b>Single Responsibility Principle (SRP)</b> by isolating
 * ownership verification logic from OTP delivery and transaction persistence concerns.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class UrbanPropertyOwnershipValidator {

    private final EncryptionDecryptionUtil encryptionDecryptionUtil;

    /**
     * Constructs {@link UrbanPropertyOwnershipValidator} with optional {@link EncryptionDecryptionUtil}
     * for decrypting stored user mobile numbers before comparing with PT owner contacts.
     *
     * @param encryptionDecryptionUtil PII decryption utility (optional)
     */
    @Autowired
    public UrbanPropertyOwnershipValidator(@Autowired(required = false) EncryptionDecryptionUtil encryptionDecryptionUtil) {
        this.encryptionDecryptionUtil = encryptionDecryptionUtil;
    }

    /**
     * Default constructor for test harnesses without encryption infrastructure.
     */
    public UrbanPropertyOwnershipValidator() {
        this(null);
    }

    /**
     * Determines whether a property is eligible for linking based on its ownership category and active links.
     *
     * @param ownershipCategory ownership classification from property-services
     * @param totalOwners total registered owners in Property Tax records
     * @param linkedCount count of citizen profiles currently linked to this property
     * @return true if property can still accept linking, false if fully linked
     */
    public boolean isEligibleForLinking(String ownershipCategory, int totalOwners, int linkedCount) {
        String cat = ownershipCategory != null ? ownershipCategory.toUpperCase() : UserConstants.CATEGORY_INDIVIDUAL;
        boolean isMultiple = cat.contains(UserConstants.CATEGORY_MULTIPLE) || cat.contains(UserConstants.CATEGORY_JOINT);
        if (!isMultiple) {
            return linkedCount == 0;
        } else {
            return linkedCount < totalOwners;
        }
    }

    /**
     * Enforces ownership capacity constraints and resolves the target owner mobile number
     * designated to receive the linking OTP.
     *
     * @param property property details fetched from Property Tax service
     * @param linkedUsers active citizen linkages found in the database
     * @param callerUuid UUID of the calling citizen initiating the linking request
     * @param requestedMobileNumber optional explicit owner mobile number specified in request
     * @return resolved destination owner mobile number
     * @throws CustomException if validation rules are violated
     */
    public String validateAndResolveTargetOwnerMobile(UrbanPropertyInfo property,
                                                      List<Map<String, Object>> linkedUsers,
                                                      String callerUuid,
                                                      String requestedMobileNumber) {
        return validateAndResolveTargetOwnerMobile(property, linkedUsers, callerUuid, requestedMobileNumber, null);
    }

    /**
     * Overloaded method accepting {@link RequestInfo} for ABAC PII decryption support.
     *
     * @param property property details fetched from Property Tax service
     * @param linkedUsers active citizen linkages found in the database
     * @param callerUuid UUID of the calling citizen initiating the linking request
     * @param requestedMobileNumber optional explicit owner mobile number specified in request
     * @param requestInfo request context envelope for ABAC decryption
     * @return resolved destination owner mobile number
     * @throws CustomException if validation rules are violated
     */
    public String validateAndResolveTargetOwnerMobile(UrbanPropertyInfo property,
                                                      List<Map<String, Object>> linkedUsers,
                                                      String callerUuid,
                                                      String requestedMobileNumber,
                                                      RequestInfo requestInfo) {
        if (property == null) {
            throw new CustomException(UserConstants.ERROR_PROPERTY_NOT_FOUND, UserConstants.MSG_URBAN_PROPERTY_NOT_FOUND_PREFIX);
        }
        if (linkedUsers == null) {
            linkedUsers = Collections.emptyList();
        }

        String propertyId = property.getPropertyId();

        // 1. Prevent duplicate linkage by caller if already linked
        if (StringUtils.isNotBlank(callerUuid)) {
            boolean callerAlreadyLinked = linkedUsers.stream()
                    .anyMatch(link -> callerUuid.equals(link.get(UserConstants.JSON_KEY_USER_UUID)));
            if (callerAlreadyLinked) {
                throw new CustomException(UserConstants.ERROR_PROPERTY_ALREADY_LINKED_TO_CALLER,
                        UserConstants.MSG_PROPERTY_ALREADY_LINKED_TO_CALLER);
            }
        }

        Set<String> linkedMobiles = linkedUsers.stream()
                .map(m -> (String) m.get(UserConstants.PT_JSON_MOBILE_NUMBER))
                .filter(StringUtils::isNotBlank)
                .map(m -> decryptMobileIfEncrypted(m, requestInfo))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());

        String category = property.getOwnershipCategory() != null ? property.getOwnershipCategory().toUpperCase() : UserConstants.CATEGORY_INDIVIDUAL;
        boolean isMultiple = category.contains(UserConstants.CATEGORY_MULTIPLE) || category.contains(UserConstants.CATEGORY_JOINT);

        if (!isMultiple) {
            // Case A: Individual Property Rule
            if (!linkedUsers.isEmpty()) {
                log.warn("[URBAN-PROPERTY-VALIDATOR] Individual property {} already linked to {} citizen(s)", propertyId, linkedUsers.size());
                throw new CustomException(UserConstants.ERROR_PROPERTY_ALREADY_LINKED,
                        UserConstants.MSG_PROPERTY_ALREADY_LINKED);
            }
            if (property.getOwners() == null || property.getOwners().isEmpty()) {
                throw new CustomException(UserConstants.ERROR_OWNER_NOT_FOUND, UserConstants.MSG_OWNER_NOT_FOUND_PREFIX + propertyId);
            }
            UrbanPropertyOwnerInfo owner = property.getOwners().get(0);
            String targetOwnerMobile = owner.getRawMobileNumber();
            if (StringUtils.isBlank(targetOwnerMobile) || targetOwnerMobile.contains(UserConstants.MASK_CHAR)) {
                throw new CustomException(UserConstants.ERROR_OWNER_MOBILE_NOT_FOUND,
                        UserConstants.MSG_OWNER_MOBILE_NOT_FOUND);
            }
            return targetOwnerMobile.trim();
        } else {
            // Case B: Multiple Owners Property Rule
            if (property.getOwners() == null || property.getOwners().isEmpty()) {
                throw new CustomException(UserConstants.ERROR_OWNER_NOT_FOUND, UserConstants.MSG_OWNER_NOT_FOUND_PREFIX + propertyId);
            }

            int totalOwners = property.getOwners().size();
            int linkedCount = linkedUsers.size();

            if (linkedCount >= totalOwners) {
                log.warn("[URBAN-PROPERTY-VALIDATOR] All owner slots filled for multi-owner property {}: {} of {}",
                        propertyId, linkedCount, totalOwners);
                throw new CustomException(UserConstants.ERROR_PROPERTY_MAX_OWNERS_EXCEEDED,
                        UserConstants.MSG_PROPERTY_MAX_OWNERS_EXCEEDED);
            }

            // Guard against masked or missing owner mobiles from PT response across all registered owners
            for (UrbanPropertyOwnerInfo owner : property.getOwners()) {
                String rawMobile = owner.getRawMobileNumber();
                if (StringUtils.isBlank(rawMobile) || rawMobile.contains(UserConstants.MASK_CHAR)) {
                    log.warn("[URBAN-PROPERTY-VALIDATOR] Owner mobile is missing or masked for multi-owner property {}: {}",
                            propertyId, MaskUtils.maskMobile(rawMobile));
                    throw new CustomException(UserConstants.ERROR_OWNER_MOBILE_NOT_FOUND,
                            UserConstants.MSG_OWNER_MOBILE_NOT_FOUND);
                }
            }

            // Identify unlinked owners
            List<UrbanPropertyOwnerInfo> unlinkedOwners = property.getOwners().stream()
                    .filter(o -> o.getRawMobileNumber() != null && !linkedMobiles.contains(o.getRawMobileNumber().trim()))
                    .collect(Collectors.toList());

            if (unlinkedOwners.isEmpty()) {
                throw new CustomException(UserConstants.ERROR_PROPERTY_MAX_OWNERS_EXCEEDED,
                        UserConstants.MSG_PROPERTY_MAX_OWNERS_EXCEEDED);
            }

            if (StringUtils.isNotBlank(requestedMobileNumber)) {
                String requestedMobile = requestedMobileNumber.trim();
                if (requestedMobile.contains(UserConstants.MASK_CHAR)) {
                    throw new CustomException(UserConstants.ERROR_INVALID_OWNER_MOBILE,
                            UserConstants.MSG_INVALID_OWNER_MOBILE_PREFIX + propertyId);
                }
                boolean matchesOwner = property.getOwners().stream()
                        .anyMatch(o -> requestedMobile.equalsIgnoreCase(o.getRawMobileNumber()));
                boolean alreadyLinked = linkedMobiles.contains(requestedMobile);
                // Mitigate Phone Oracle vulnerability: return uniform error whether mobile is non-owner or already linked
                if (!matchesOwner || alreadyLinked) {
                    throw new CustomException(UserConstants.ERROR_INVALID_OWNER_MOBILE,
                            UserConstants.MSG_INVALID_OWNER_MOBILE_PREFIX + propertyId);
                }
                return requestedMobile;
            } else {
                if (unlinkedOwners.size() == 1) {
                    String targetMobile = unlinkedOwners.get(0).getRawMobileNumber();
                    if (StringUtils.isBlank(targetMobile) || targetMobile.contains(UserConstants.MASK_CHAR)) {
                        throw new CustomException(UserConstants.ERROR_OWNER_MOBILE_NOT_FOUND,
                                UserConstants.MSG_OWNER_MOBILE_NOT_FOUND);
                    }
                    return targetMobile.trim();
                } else {
                    throw new CustomException(UserConstants.ERROR_OWNER_SELECTION_REQUIRED,
                            UserConstants.MSG_OWNER_SELECTION_REQUIRED);
                }
            }
        }
    }

    private String decryptMobileIfEncrypted(String rawMobile, RequestInfo requestInfo) {
        if (StringUtils.isBlank(rawMobile)) {
            return rawMobile;
        }
        if (encryptionDecryptionUtil != null && rawMobile.contains(UserConstants.ENCRYPTION_DELIMITER)) {
            try {
                Map<String, String> dummyUser = new HashMap<>();
                dummyUser.put(UserConstants.JSON_KEY_MOBILE_NUMBER, rawMobile.trim());
                Map<String, Object> decrypted = encryptionDecryptionUtil.decryptObject(dummyUser, UserConstants.USER_ENCRYPTION_KEY, Map.class, requestInfo);
                if (decrypted != null && decrypted.get(UserConstants.JSON_KEY_MOBILE_NUMBER) != null) {
                    return String.valueOf(decrypted.get(UserConstants.JSON_KEY_MOBILE_NUMBER)).trim();
                }
            } catch (Exception e) {
                log.debug("[URBAN-PROPERTY-VALIDATOR] Failed to decrypt user mobile: {}", e.getMessage());
            }
        }
        return rawMobile.trim();
    }
}
