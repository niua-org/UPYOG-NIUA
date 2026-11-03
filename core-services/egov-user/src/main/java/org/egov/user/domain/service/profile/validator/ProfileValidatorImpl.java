package org.egov.user.domain.service.profile.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.UserRequest;
import org.egov.user.web.contract.profile.ProfileCreateRequest;
import org.egov.user.web.contract.profile.ProfileInitRequest;
import org.egov.user.web.contract.profile.ProfileResyncRequest;
import org.egov.user.web.contract.profile.ProfileSearchRequest;
import org.egov.user.web.contract.profile.ProfileUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
/**
 * Implementation of {@link ProfileValidator} enforcing Section 8 mandatory field requirements
 * and non-editable field restrictions across profile workflows.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class ProfileValidatorImpl implements ProfileValidator {

    @Autowired(required = false)
    private UserRepository userRepository;

    /**
     * Determines whether the authenticated caller is a CITIZEN as per the database record.
     * Checks database ground truth first; falls back to requestInfo token type if user is not in DB.
     *
     * @param callerInfo user contract from requestInfo
     * @return true if caller is a CITIZEN, false otherwise
     */
    public boolean isCallerCitizen(org.egov.common.contract.request.User callerInfo) {
        if (callerInfo == null) {
            return false;
        }
        if (userRepository != null) {
            if (StringUtils.isNotBlank(callerInfo.getUuid())) {
                try {
                    UserSearchCriteria criteria = UserSearchCriteria.builder()
                            .uuid(Collections.singletonList(callerInfo.getUuid()))
                            .excludeAddressDetails(true)
                            .build();
                    List<User> list = userRepository.findAll(criteria);
                    if (!CollectionUtils.isEmpty(list)) {
                        return UserType.CITIZEN.equals(list.get(0).getType());
                    }
                } catch (Exception e) {
                    log.debug("[PROFILE-VALIDATOR] Error verifying caller type in DB by uuid: {}", e.getMessage());
                }
            } else if (callerInfo.getId() != null) {
                try {
                    UserSearchCriteria criteria = UserSearchCriteria.builder()
                            .id(Collections.singletonList(callerInfo.getId()))
                            .excludeAddressDetails(true)
                            .build();
                    List<User> list = userRepository.findAll(criteria);
                    if (!CollectionUtils.isEmpty(list)) {
                        return UserType.CITIZEN.equals(list.get(0).getType());
                    }
                } catch (Exception e) {
                    log.debug("[PROFILE-VALIDATOR] Error verifying caller type in DB by id: {}", e.getMessage());
                }
            }
        }
        return UserType.CITIZEN.name().equalsIgnoreCase(callerInfo.getType());
    }

    @Override
    public void validateInit(ProfileInitRequest request) {
        if (request == null) {
            log.error("[PROFILE-VALIDATOR] Profile init failed: request body is null");
            throw new CustomException(UserConstants.ERROR_INVALID_INIT_REQUEST, UserConstants.MSG_INIT_BODY_NULL);
        }
        request.unpackUserIfPresent();
        if (StringUtils.isBlank(request.getTenantId())) {
            log.error("[PROFILE-VALIDATOR] Profile init failed: tenantId is missing");
            throw new CustomException(UserConstants.ERROR_TENANT_ID_REQUIRED, UserConstants.MSG_INIT_TENANT_REQUIRED);
        }
        if (StringUtils.isBlank(request.getMobileNumber())) {
            log.error("[PROFILE-VALIDATOR] Profile init failed: mobileNumber is missing");
            throw new CustomException(UserConstants.ERROR_MOBILE_NUMBER_REQUIRED, UserConstants.MSG_INIT_MOBILE_REQUIRED);
        }
    }

    @Override
    public void validateCreate(ProfileCreateRequest request) {
        if (request == null) {
            log.error("[PROFILE-VALIDATOR] Profile create failed: request body is null");
            throw new CustomException(UserConstants.ERROR_INVALID_CREATE_REQUEST, UserConstants.MSG_CREATE_BODY_NULL);
        }

        request.unpackUserIfPresent();

        if (StringUtils.isBlank(request.getTenantId())) {
            log.error("[PROFILE-VALIDATOR] Profile create failed: tenantId is missing");
            throw new CustomException(UserConstants.ERROR_TENANT_ID_REQUIRED, UserConstants.MSG_CREATE_TENANT_REQUIRED);
        }

        // 1. Mandatory Demographics Validation
        if (StringUtils.isBlank(request.getName())) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_NAME_MANDATORY, UserConstants.MSG_NAME_MANDATORY);
        }
        if (StringUtils.isBlank(request.getFatherName())) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_FATHER_NAME_MANDATORY, UserConstants.MSG_FATHER_NAME_MANDATORY);
        }
        if (StringUtils.isBlank(request.getMotherName())) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_MOTHER_NAME_MANDATORY, UserConstants.MSG_MOTHER_NAME_MANDATORY);
        }
        if (StringUtils.isBlank(request.getMobileNumber())) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_MOBILE_MANDATORY, UserConstants.MSG_MOBILE_MANDATORY);
        }
        if (request.getDob() == null) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_DOB_MANDATORY, UserConstants.MSG_DOB_MANDATORY);
        }
        if (StringUtils.isBlank(request.getEmailId())) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_EMAIL_MANDATORY, UserConstants.MSG_EMAIL_MANDATORY);
        }
        if (StringUtils.isBlank(request.getGender())) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_GENDER_MANDATORY, UserConstants.MSG_GENDER_MANDATORY);
        }

        // Minor Citizen Guardian Check: If citizen is under 18 years old, Guardian details are mandatory
        if (isMinor(request.getDob())) {
            if (StringUtils.isBlank(request.getGuardian()) || StringUtils.isBlank(request.getGuardianRelation())) {
                log.warn("[PROFILE-VALIDATOR] Minor citizen (under 18) must have declared Guardian Name and Relation");
                throw new CustomException(UserConstants.ERROR_GUARDIAN_MANDATORY_FOR_MINOR, UserConstants.MSG_GUARDIAN_MANDATORY_FOR_MINOR);
            }
        }

        // Alternate Mobile Validation: If provided, must match Indian 10-digit mobile pattern and differ from primary
        if (StringUtils.isNotBlank(request.getAlternateMobileNumber())) {
            String altMobile = request.getAlternateMobileNumber().trim();
            if (!altMobile.matches(UserConstants.REGEX_INDIAN_MOBILE)) {
                log.warn("[PROFILE-VALIDATOR] Invalid alternate mobile number: {}", altMobile);
                throw new CustomException(UserConstants.ERROR_INVALID_ALTERNATE_MOBILE, UserConstants.MSG_INVALID_ALTERNATE_MOBILE);
            }
            if (altMobile.equals(request.getMobileNumber() != null ? request.getMobileNumber().trim() : "")) {
                log.warn("[PROFILE-VALIDATOR] Alternate mobile number is identical to primary mobile number: {}", altMobile);
                throw new CustomException(UserConstants.ERROR_ALTERNATE_MOBILE_SAME_AS_PRIMARY, UserConstants.MSG_ALTERNATE_MOBILE_SAME_AS_PRIMARY);
            }
        }

        // 2. Mandatory Address Validation
        // 2. Mandatory Address Validation
        List<Address> effectiveAddrs = request.getEffectiveAddresses();
        if (CollectionUtils.isEmpty(effectiveAddrs)) {
            throw new CustomException(UserConstants.UG_USER_PROFILE_ADDRESS_MANDATORY, UserConstants.MSG_ADDRESS_MANDATORY);
        }
        for (Address addr : effectiveAddrs) {
            if (addr != null) {
                addr.validateMandatoryAddressFields();
            }
        }

        // 3. Mandatory Document Proofs Validation
        // Address Proof: Each address has its own address proof in the address object (filestoreId)
        // or fallback to request-level addressProofFileStoreId / documents list
        String fallbackAddressProof = StringUtils.isNotBlank(request.getAddressProofFileStoreId())
                ? request.getAddressProofFileStoreId()
                : findFilestoreIdForDocType(request.getDocuments(), UserConstants.ADDRESS_PROOF_TYPES);

        for (Address addr : effectiveAddrs) {
            if (addr != null) {
                if (StringUtils.isBlank(addr.getFilestoreId())) {
                    if (StringUtils.isNotBlank(fallbackAddressProof)) {
                        addr.setFilestoreId(fallbackAddressProof);
                    } else {
                        String typeStr = addr.getType() != null ? addr.getType().name() : (addr.getAddressType() != null ? addr.getAddressType() : "Address");
                        log.error("[PROFILE-VALIDATOR] Upload Address Proof is mandatory for address: {}", typeStr);
                        throw new CustomException(UserConstants.UG_USER_DOC_ADDRESS_PROOF_MANDATORY,
                                "Upload Address Proof is mandatory for " + typeStr + ".");
                    }
                }
            }
        }

        // Identity Proof
        boolean hasIdentityProof = StringUtils.isNotBlank(request.getIdentityProofFileStoreId())
                || containsDocumentType(request.getDocuments(), UserConstants.IDENTITY_PROOF_TYPES);
        if (!hasIdentityProof) {
            throw new CustomException(UserConstants.UG_USER_DOC_IDENTITY_PROOF_MANDATORY, UserConstants.MSG_IDENTITY_PROOF_MANDATORY);
        }

        // D.O.B. Proof
        boolean hasDobProof = StringUtils.isNotBlank(request.getDobProofFileStoreId())
                || containsDocumentType(request.getDocuments(), UserConstants.DOB_PROOF_TYPES);
        if (!hasDobProof) {
            throw new CustomException(UserConstants.UG_USER_DOC_DOB_PROOF_MANDATORY, UserConstants.MSG_DOB_PROOF_MANDATORY);
        }
    }

    @Override
    public void validateUpdate(ProfileUpdateRequest request, User existingUser) {
        if (request == null) {
            log.error("[PROFILE-VALIDATOR] Profile update failed: request body is null");
            throw new CustomException(UserConstants.ERROR_INVALID_UPDATE_REQUEST, "Profile update request body cannot be null");
        }
        if (existingUser == null) {
            log.error("[PROFILE-VALIDATOR] Profile update failed: target citizen record not found");
            throw new CustomException(UserConstants.ERROR_USER_NOT_FOUND, "Citizen record not found for update");
        }

        UserRequest incomingProfile = request.getEffectiveProfile();
        if (incomingProfile == null) {
            log.error("[PROFILE-VALIDATOR] Profile update failed: profile payload is null");
            throw new CustomException(UserConstants.ERROR_INVALID_PROFILE_PAYLOAD, "Profile payload cannot be null");
        }

        // 1. Validate ID and UUID Consistency
        if (incomingProfile.getId() != null && existingUser.getId() != null
                && !incomingProfile.getId().equals(existingUser.getId())) {
            log.error("[PROFILE-VALIDATOR] Data mismatch: incoming id {} does not match existingUser id {}",
                    incomingProfile.getId(), existingUser.getId());
            throw new CustomException(UserConstants.ERROR_DATA_MISMATCH,
                    "Data mismatch: Provided id " + incomingProfile.getId() + " does not match target citizen id " + existingUser.getId());
        }
        if (StringUtils.isNotBlank(incomingProfile.getUuid()) && StringUtils.isNotBlank(existingUser.getUuid())
                && !incomingProfile.getUuid().equalsIgnoreCase(existingUser.getUuid())) {
            log.error("[PROFILE-VALIDATOR] Data mismatch: incoming uuid {} does not match existingUser uuid {}",
                    incomingProfile.getUuid(), existingUser.getUuid());
            throw new CustomException(UserConstants.ERROR_DATA_MISMATCH,
                    "Data mismatch: Provided uuid " + incomingProfile.getUuid() + " does not match target citizen uuid " + existingUser.getUuid());
        }

        // Caller authorization: If caller is a CITIZEN as per DB, they can only update their own profile
        if (request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null
                && isCallerCitizen(request.getRequestInfo().getUserInfo())) {
            org.egov.common.contract.request.User caller = request.getRequestInfo().getUserInfo();
            String callerUuid = caller.getUuid();
            Long callerId = caller.getId();
            boolean uuidMismatch = StringUtils.isNotBlank(callerUuid) && StringUtils.isNotBlank(existingUser.getUuid())
                    && !callerUuid.equalsIgnoreCase(existingUser.getUuid());
            boolean idMismatch = callerId != null && existingUser.getId() != null
                    && !callerId.equals(existingUser.getId());
            if (uuidMismatch || idMismatch) {
                log.error("[PROFILE-VALIDATOR] Unauthorized update: Citizen caller {} attempted to update profile of citizen {}",
                        callerUuid, existingUser.getUuid());
                throw new CustomException("UNAUTHORIZED_ACCESS", "Citizen users are only permitted to update their own profile");
            }
        }

        boolean isResync = Boolean.TRUE.equals(request.getIsResync());

        // 2. ENFORCE SECTION 8 NON-EDITABLE DEMOGRAPHIC FIELDS:
        // A. Mobile Number (Non-Editable)
        if (isFieldModified(incomingProfile.getMobileNumber(), existingUser.getMobileNumber())) {
            log.error("[PROFILE-VALIDATOR] Non-editable restriction violated: Cannot edit Mobile Number: uuid={}", existingUser.getUuid());
            throw new CustomException(UserConstants.UG_USER_PROFILE_NON_EDITABLE_MOBILE, UserConstants.MSG_NON_EDITABLE_MOBILE);
        }

        // B. Father's Name (Non-Editable)
        if (isFieldModified(incomingProfile.getFatherName(), existingUser.getFatherName())) {
            log.error("[PROFILE-VALIDATOR] Non-editable restriction violated: Cannot edit Father's Name: uuid={}", existingUser.getUuid());
            throw new CustomException(UserConstants.UG_USER_PROFILE_NON_EDITABLE_FATHER_NAME, UserConstants.MSG_NON_EDITABLE_FATHER_NAME);
        }

        // C. Mother's Name (Non-Editable)
        if (isFieldModified(incomingProfile.getMotherName(), existingUser.getMotherName())) {
            log.error("[PROFILE-VALIDATOR] Non-editable restriction violated: Cannot edit Mother's Name: uuid={}", existingUser.getUuid());
            throw new CustomException(UserConstants.UG_USER_PROFILE_NON_EDITABLE_MOTHER_NAME, UserConstants.MSG_NON_EDITABLE_MOTHER_NAME);
        }

        // D. Email ID (Non-Editable)
        if (isFieldModified(incomingProfile.getEmailId(), existingUser.getEmailId())) {
            log.error("[PROFILE-VALIDATOR] Non-editable restriction violated: Cannot edit Email ID: uuid={}", existingUser.getUuid());
            throw new CustomException(UserConstants.UG_USER_PROFILE_NON_EDITABLE_EMAIL, UserConstants.MSG_NON_EDITABLE_EMAIL);
        }

        // E. Name (Non-Editable unless Aadhaar re-sync)
        if (!isResync && isFieldModified(incomingProfile.getName(), existingUser.getName())) {
            log.error("[PROFILE-VALIDATOR] Non-editable restriction violated: Cannot edit Name without re-sync: uuid={}", existingUser.getUuid());
            throw new CustomException(UserConstants.UG_USER_PROFILE_NON_EDITABLE_NAME, UserConstants.MSG_NON_EDITABLE_NAME);
        }

        // F. Date of Birth (Non-Editable unless Aadhaar re-sync)
        if (!isResync && existingUser.getDob() != null && incomingProfile.getDob() != null
                && !DateUtils.isSameDay(incomingProfile.getDob(), existingUser.getDob())) {
            log.error("[PROFILE-VALIDATOR] Non-editable restriction violated: Cannot edit DOB without re-sync: uuid={}", existingUser.getUuid());
            throw new CustomException(UserConstants.UG_USER_PROFILE_NON_EDITABLE_DOB, UserConstants.MSG_NON_EDITABLE_DOB);
        }

        // G. Gender (Non-Editable unless Aadhaar re-sync)
        if (!isResync && existingUser.getGender() != null && StringUtils.isNotBlank(incomingProfile.getGender())
                && !incomingProfile.getGender().trim().equalsIgnoreCase(existingUser.getGender().name())) {
            log.error("[PROFILE-VALIDATOR] Non-editable restriction violated: Cannot edit Gender without re-sync: uuid={}", existingUser.getUuid());
            throw new CustomException(UserConstants.UG_USER_PROFILE_NON_EDITABLE_GENDER, UserConstants.MSG_NON_EDITABLE_GENDER);
        }

        // H. Alternate Mobile Validation on Update
        if (StringUtils.isNotBlank(incomingProfile.getAlternateMobileNumber())) {
            String altMobile = incomingProfile.getAlternateMobileNumber().trim();
            if (!altMobile.matches(UserConstants.REGEX_INDIAN_MOBILE)) {
                log.warn("[PROFILE-VALIDATOR] Invalid alternate mobile number during update: {}", altMobile);
                throw new CustomException(UserConstants.ERROR_INVALID_ALTERNATE_MOBILE, UserConstants.MSG_INVALID_ALTERNATE_MOBILE);
            }
            if (altMobile.equals(existingUser.getMobileNumber() != null ? existingUser.getMobileNumber().trim() : "")) {
                log.warn("[PROFILE-VALIDATOR] Alternate mobile number cannot match primary mobile number: {}", altMobile);
                throw new CustomException(UserConstants.ERROR_ALTERNATE_MOBILE_SAME_AS_PRIMARY, UserConstants.MSG_ALTERNATE_MOBILE_SAME_AS_PRIMARY);
            }
        }

        // I. Minor Citizen Guardian Protection: Minors (<18) cannot have their guardian cleared
        Date effectiveDob = (incomingProfile.getDob() != null) ? incomingProfile.getDob() : existingUser.getDob();
        if (isMinor(effectiveDob)) {
            String newGuardian = incomingProfile.getGuardian();
            String newRelation = incomingProfile.getGuardianRelation();
            if ((newGuardian != null && StringUtils.isBlank(newGuardian))
                    || (newRelation != null && StringUtils.isBlank(newRelation))) {
                log.warn("[PROFILE-VALIDATOR] Cannot remove guardian from minor citizen profile: uuid={}", existingUser.getUuid());
                throw new CustomException(UserConstants.ERROR_GUARDIAN_MANDATORY_FOR_MINOR, UserConstants.MSG_GUARDIAN_MANDATORY_FOR_MINOR);
            }
        }

        // If isResync is true, verify that OTP is present
        if (isResync && StringUtils.isBlank(request.getOtp())) {
            log.error("[PROFILE-VALIDATOR] Re-sync validation failed: OTP is missing");
            throw new CustomException(UserConstants.ERROR_OTP_MANDATORY_FOR_RESYNC, UserConstants.MSG_OTP_MANDATORY_FOR_RESYNC);
        }
    }

    @Override
    public void validateSearch(ProfileSearchRequest request) {
        if (request == null) {
            log.error("[PROFILE-VALIDATOR] Profile search failed: request body is null");
            throw new CustomException(UserConstants.ERROR_INVALID_SEARCH_REQUEST, "Profile search request body cannot be null");
        }
        boolean hasFilter = StringUtils.isNotBlank(request.getTenantId())
                || !CollectionUtils.isEmpty(request.getUuid())
                || !CollectionUtils.isEmpty(request.getId())
                || StringUtils.isNotBlank(request.getMobileNumber())
                || StringUtils.isNotBlank(request.getAadhaarNumber())
                || StringUtils.isNotBlank(request.getName());

        if (!hasFilter) {
            log.error("[PROFILE-VALIDATOR] Profile search failed: no search criteria supplied");
            throw new CustomException(UserConstants.ERROR_EMPTY_SEARCH_CRITERIA, UserConstants.MSG_EMPTY_SEARCH_CRITERIA);
        }

        // CITIZEN authorization: A citizen (as per DB ground truth) is only permitted to search for their own profile
        if (request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null
                && isCallerCitizen(request.getRequestInfo().getUserInfo())) {
            validateCitizenSelfSearch(request.getRequestInfo().getUserInfo(),
                    request.getUuid(), request.getMobileNumber(), request.getId());
        }
    }

    private void validateCitizenSelfSearch(org.egov.common.contract.request.User caller,
                                          List<String> uuidList, String mobileNumber, List<Long> idList) {
        User callerDbUser = null;
        if (userRepository != null && StringUtils.isNotBlank(caller.getUuid())) {
            try {
                UserSearchCriteria criteria = UserSearchCriteria.builder()
                        .uuid(Collections.singletonList(caller.getUuid()))
                        .excludeAddressDetails(true)
                        .build();
                List<User> list = userRepository.findAll(criteria);
                if (!CollectionUtils.isEmpty(list)) {
                    callerDbUser = list.get(0);
                }
            } catch (Exception ignored) {}
        }

        String callerUuid = (callerDbUser != null && StringUtils.isNotBlank(callerDbUser.getUuid()))
                ? callerDbUser.getUuid() : caller.getUuid();
        String callerMobile = (callerDbUser != null && StringUtils.isNotBlank(callerDbUser.getMobileNumber()))
                ? callerDbUser.getMobileNumber().trim()
                : (StringUtils.isNotBlank(caller.getMobileNumber())
                ? caller.getMobileNumber().trim()
                : (StringUtils.isNotBlank(caller.getUserName()) && StringUtils.isNumeric(caller.getUserName().trim())
                ? caller.getUserName().trim() : null));
        Long callerId = (callerDbUser != null && callerDbUser.getId() != null)
                ? callerDbUser.getId() : caller.getId();
        String callerUserName = (callerDbUser != null && StringUtils.isNotBlank(callerDbUser.getUsername()))
                ? callerDbUser.getUsername() : caller.getUserName();

        boolean hasUuid = !CollectionUtils.isEmpty(uuidList);
        boolean hasMobile = StringUtils.isNotBlank(mobileNumber);
        boolean hasId = !CollectionUtils.isEmpty(idList);

        // Citizen must supply at least one identifying criterion (uuid, mobileNumber, or id)
        if (!hasUuid && !hasMobile && !hasId) {
            log.error("[PROFILE-VALIDATOR] Unauthorized search: Citizen caller must search using their own UUID or mobile number.");
            throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, UserConstants.MSG_UNAUTHORIZED_CITIZEN_SEARCH);
        }

        // Validate UUIDs: must match caller's UUID
        if (hasUuid) {
            if (StringUtils.isBlank(callerUuid)) {
                log.error("[PROFILE-VALIDATOR] Unauthorized search: Calling citizen has no UUID in request context.");
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, UserConstants.MSG_UNAUTHORIZED_CITIZEN_SEARCH);
            }
            boolean anyMismatch = uuidList.stream()
                    .filter(StringUtils::isNotBlank)
                    .anyMatch(u -> !u.trim().equalsIgnoreCase(callerUuid.trim()));
            if (anyMismatch) {
                log.error("[PROFILE-VALIDATOR] Unauthorized search: Citizen caller {} attempted to search for uuid(s): {}", callerUuid, uuidList);
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, UserConstants.MSG_UNAUTHORIZED_CITIZEN_SEARCH);
            }
        }

        // Validate Mobile Number: must match caller's mobile or username
        if (hasMobile) {
            String trimmedMobile = mobileNumber.trim();
            boolean matchesCaller = (callerMobile != null && trimmedMobile.equals(callerMobile))
                    || (callerUserName != null && trimmedMobile.equalsIgnoreCase(callerUserName.trim()));
            if (!matchesCaller) {
                log.error("[PROFILE-VALIDATOR] Unauthorized search: Citizen caller {} attempted to search for mobile: {}", callerUuid, trimmedMobile);
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, UserConstants.MSG_UNAUTHORIZED_CITIZEN_SEARCH);
            }
        }

        // Validate ID: must match caller's ID
        if (hasId) {
            if (callerId == null) {
                log.error("[PROFILE-VALIDATOR] Unauthorized search: Calling citizen has no ID in request context.");
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, UserConstants.MSG_UNAUTHORIZED_CITIZEN_SEARCH);
            }
            boolean anyMismatch = idList.stream()
                    .filter(Objects::nonNull)
                    .anyMatch(id -> !id.equals(callerId));
            if (anyMismatch) {
                log.error("[PROFILE-VALIDATOR] Unauthorized search: Citizen caller ID {} attempted to search for id(s): {}", callerId, idList);
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_CITIZEN_SEARCH, UserConstants.MSG_UNAUTHORIZED_CITIZEN_SEARCH);
            }
        }
    }

    @Override
    public void validateResync(ProfileResyncRequest request, User existingUser) {
        if (request == null) {
            log.error("[PROFILE-VALIDATOR] Profile re-sync failed: request body is null");
            throw new CustomException(UserConstants.ERROR_INVALID_RESYNC_REQUEST, "Profile re-sync request body cannot be null");
        }
        request.unpackUserIfPresent();
        if (existingUser == null) {
            log.error("[PROFILE-VALIDATOR] Profile re-sync failed: target citizen record not found");
            throw new CustomException(UserConstants.ERROR_USER_NOT_FOUND, "Citizen record not found for Aadhaar re-sync");
        }
        if (StringUtils.isBlank(request.getTenantId())) {
            log.error("[PROFILE-VALIDATOR] Profile re-sync failed: tenantId is missing");
            throw new CustomException(UserConstants.ERROR_TENANT_ID_REQUIRED, "tenantId is mandatory for profile re-sync");
        }
        if (StringUtils.isBlank(request.getOtp())) {
            log.error("[PROFILE-VALIDATOR] Profile re-sync failed: otp is missing");
            throw new CustomException(UserConstants.ERROR_OTP_MANDATORY_FOR_RESYNC, UserConstants.MSG_OTP_MANDATORY_FOR_RESYNC);
        }

        // 1. Authorization check: logged-in user must match target citizen profile being re-synced
        if (request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null) {
            String loggedInUuid = request.getRequestInfo().getUserInfo().getUuid();
            if (StringUtils.isNotBlank(loggedInUuid) && StringUtils.isNotBlank(existingUser.getUuid())
                    && !loggedInUuid.equalsIgnoreCase(existingUser.getUuid())) {
                log.error("[PROFILE-VALIDATOR] Unauthorized re-sync attempt: logged-in user uuid {} does not match target profile uuid {}",
                        loggedInUuid, existingUser.getUuid());
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_RESYNC, UserConstants.MSG_UNAUTHORIZED_RESYNC);
            }
        }

        // 2. Aadhaar consistency check: cannot re-sync profile with a different Aadhaar number than previously registered
        if (StringUtils.isNotBlank(request.getAadhaarNumber()) && StringUtils.isNotBlank(existingUser.getAadhaarNumber())) {
            String existingAadhaar = existingUser.getAadhaarNumber().replaceAll("\\s+", "");
            String incomingAadhaar = request.getAadhaarNumber().replaceAll("\\s+", "");
            if (existingAadhaar.length() >= 4 && incomingAadhaar.length() >= 4) {
                String existingLast4 = existingAadhaar.substring(existingAadhaar.length() - 4);
                String incomingLast4 = incomingAadhaar.substring(incomingAadhaar.length() - 4);
                if (!existingLast4.equals(incomingLast4)) {
                    log.error("[PROFILE-VALIDATOR] Aadhaar mismatch: citizen registered Aadhaar ends with {}, incoming ends with {}",
                            existingLast4, incomingLast4);
                    throw new CustomException(UserConstants.ERROR_AADHAAR_CANNOT_BE_CHANGED, UserConstants.MSG_AADHAAR_MISMATCH);
                }
            }
        }
    }

    /**
     * Determines whether an incoming string attribute differs from the existing persisted value.
     * Whitespace differences are normalized, and comparison is case-insensitive.
     *
     * @param incoming the new incoming string
     * @param existing the current persisted string
     * @return true if the field is meaningfully modified, false otherwise
     */
    private boolean isFieldModified(String incoming, String existing) {
        if (StringUtils.isBlank(existing)) {
            return false;
        }
        if (StringUtils.isBlank(incoming)) {
            return false;
        }
        return !incoming.trim().equalsIgnoreCase(existing.trim());
    }

    /**
     * Checks if the user documents list contains at least one active document matching the allowed types
     * and having a valid filestoreId.
     *
     * @param documents    list of attached user documents
     * @param allowedTypes list of accepted document type identifiers
     * @return true if an allowed document type is present with valid fileStoreId, false otherwise
     */
    private boolean containsDocumentType(List<UserDocument> documents, List<String> allowedTypes) {
        if (CollectionUtils.isEmpty(documents)) {
            return false;
        }
        return documents.stream().anyMatch(doc ->
                doc != null
                        && StringUtils.isNotBlank(doc.getDocumentType())
                        && allowedTypes.contains(doc.getDocumentType().toUpperCase())
                        && StringUtils.isNotBlank(doc.getFilestoreId())
        );
    }

    private String findFilestoreIdForDocType(List<UserDocument> documents, List<String> allowedTypes) {
        if (CollectionUtils.isEmpty(documents)) {
            return null;
        }
        for (UserDocument doc : documents) {
            if (doc != null
                    && StringUtils.isNotBlank(doc.getDocumentType())
                    && allowedTypes.contains(doc.getDocumentType().toUpperCase())
                    && StringUtils.isNotBlank(doc.getFilestoreId())) {
                return doc.getFilestoreId();
            }
        }
        return null;
    }

    /**
     * Computes whether the provided date of birth corresponds to a minor citizen under 18 years of age.
     *
     * @param dob the date of birth
     * @return true if citizen is under 18 years old, false otherwise
     */
    private boolean isMinor(Date dob) {
        if (dob == null) {
            return false;
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -18);
        return dob.after(cal.getTime());
    }
}
