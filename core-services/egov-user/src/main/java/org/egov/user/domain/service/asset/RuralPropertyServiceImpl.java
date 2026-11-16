package org.egov.user.domain.service.asset;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.asset.PropertyOwner;
import org.egov.user.domain.model.asset.RuralPropertyDetail;
import org.egov.user.domain.model.asset.UserProperty;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserPropertyRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.asset.RuralPropertyRequest;
import org.egov.user.web.contract.asset.RuralPropertySearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation managing standalone CRUD operations and lifecycle status mutations for citizen rural properties.
 *
 * <p>Enforces:</p>
 * <ul>
 *   <li>Citizen existence validation in core {@code eg_user} registry.</li>
 *   <li>Mandatory Gram Panchayat and Village name requirements for rural properties.</li>
 *   <li>Ownership security boundaries preventing cross-citizen property updates or deletions.</li>
 *   <li>Soft-deactivation setting {@code status = INACTIVE} with complete audit trail persistence.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class RuralPropertyServiceImpl implements RuralPropertyService {

    private final UserPropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final UserDocumentRepository documentRepository;

    /**
     * Constructs {@link RuralPropertyServiceImpl} with required persistence repositories.
     *
     * @param propertyRepository persistence repository for 3NF property tables
     * @param userRepository     repository for querying citizen user existence
     * @param documentRepository repository for storing attached title and verification documents
     */
    @Autowired
    public RuralPropertyServiceImpl(UserPropertyRepository propertyRepository,
                                    UserRepository userRepository,
                                    UserDocumentRepository documentRepository) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * Creates a new rural property declaration for a citizen.
     *
     * @param request rural property creation request payload
     * @return persisted {@link UserProperty} domain entity with populated IDs and audit timestamps
     */
    @Override
    @Transactional
    public UserProperty createRuralProperty(RuralPropertyRequest request) {
        if (request == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_PROPERTY_REQUEST, "Request payload cannot be null");
        }

        UserProperty prop = request.resolveProperty();
        String userUuid = prop.getUserUuid();
        if (StringUtils.isBlank(userUuid)) {
            throw new CustomException(UserConstants.ERROR_USER_UUID_REQUIRED, UserConstants.MSG_CITIZEN_USER_UUID_REQUIRED);
        }

        // Validate citizen exists in eg_user
        User existingCitizen = validateCitizenExists(userUuid);

        RuralPropertyDetail rural = prop.getRuralPropertyDetail();
        if (rural == null || StringUtils.isBlank(rural.getGramPanchayat()) || StringUtils.isBlank(rural.getVillageName())
                || StringUtils.isBlank(prop.getTenantId())) {
            throw new CustomException(UserConstants.ERROR_RURAL_PROPERTY_MANDATORY_FIELDS, UserConstants.MSG_RURAL_MANDATORY_FIELDS);
        }

        String actorUuid = resolveActorUuid(request.getRequestInfo());

        // Natural key deduplication by survey number if ID is not explicitly provided
        if (StringUtils.isBlank(prop.getId())) {
            if (StringUtils.isNotBlank(rural.getSurveyNumber())) {
                String existingId = propertyRepository.findRuralPropertyId(userUuid, rural.getSurveyNumber());
                if (existingId != null) {
                    prop.setId(existingId);
                }
            }
            if (StringUtils.isBlank(prop.getId())) {
                prop.setId(UUID.randomUUID().toString());
            }
        }

        prop.setUserUuid(userUuid);
        prop.setLocalityType(UserConstants.LOCALITY_TYPE_RURAL);
        if (StringUtils.isBlank(prop.getStatus())) {
            prop.setStatus(UserConstants.STATUS_ACTIVE_UPPERCASE);
        }
        prop.setCreatedBy(actorUuid);
        prop.setLastModifiedBy(actorUuid);

        rural.setPropertyRecordId(prop.getId());
        rural.setId(prop.getId());
        rural.setTenantId(prop.getTenantId());
        rural.setCreatedBy(actorUuid);
        rural.setLastModifiedBy(actorUuid);

        // Attach verification document if supplied
        if (prop.getDocument() != null && StringUtils.isNotBlank(prop.getDocument().getFilestoreId())) {
            UserDocument doc = prop.getDocument();
            doc.setUserId(existingCitizen.getId());
            doc.setUserUuid(existingCitizen.getUuid());
            doc.setTenantId(prop.getTenantId());
            doc.setReferenceType(UserConstants.DOC_REF_TYPE_PROPERTY);
            doc.setReferenceId(prop.getId());
            doc.setCreatedBy(actorUuid);
            doc.setLastModifiedBy(actorUuid);
            documentRepository.create(doc);
            prop.setDocumentId(doc.getId());
        }

        propertyRepository.saveProperty(prop);
        propertyRepository.saveRuralProperty(rural);

        // Persist co-owners if present
        if (!CollectionUtils.isEmpty(prop.getOwners())) {
            for (PropertyOwner owner : prop.getOwners()) {
                if (owner == null) continue;
                if (StringUtils.isBlank(owner.getId())) {
                    owner.setId(UUID.randomUUID().toString());
                }
                owner.setPropertyId(prop.getId());
                owner.setTenantId(prop.getTenantId());
                owner.setCreatedBy(actorUuid);
                owner.setLastModifiedBy(actorUuid);
                propertyRepository.savePropertyOwner(owner);
            }
        }

        log.info("[RURAL-PROP-SERVICE] Created rural property ID: {} for citizen UUID: {}", prop.getId(), userUuid);
        return propertyRepository.findRuralPropertyById(prop.getId());
    }

    /**
     * Updates an existing rural property declaration for a citizen.
     *
     * @param request rural property update request payload
     * @return updated {@link UserProperty} domain entity
     */
    @Override
    @Transactional
    public UserProperty updateRuralProperty(RuralPropertyRequest request) {
        if (request == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_PROPERTY_REQUEST, "Request payload cannot be null");
        }

        String propertyId = request.resolvePropertyId();
        if (StringUtils.isBlank(propertyId)) {
            throw new CustomException(UserConstants.ERROR_INVALID_PROPERTY_REQUEST, "Property ID is mandatory for update");
        }

        String userUuid = request.resolveUserUuid();
        if (StringUtils.isBlank(userUuid)) {
            throw new CustomException(UserConstants.ERROR_USER_UUID_REQUIRED, UserConstants.MSG_CITIZEN_USER_UUID_REQUIRED);
        }

        UserProperty existing = propertyRepository.findRuralPropertyById(propertyId);
        if (existing == null) {
            throw new CustomException(UserConstants.ERROR_PROPERTY_NOT_FOUND, UserConstants.MSG_PROPERTY_NOT_FOUND + propertyId);
        }

        // Enforce ownership boundary
        if (!userUuid.equals(existing.getUserUuid())) {
            log.warn("[RURAL-PROP-SERVICE] Ownership mismatch: property {} belongs to {} but update was requested by {}",
                    propertyId, existing.getUserUuid(), userUuid);
            throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_ACCESS, UserConstants.MSG_PROPERTY_ACCESS_DENIED);
        }

        String actorUuid = resolveActorUuid(request.getRequestInfo());

        UserProperty incoming = request.resolveProperty();

        // Merge base properties
        if (StringUtils.isNotBlank(incoming.getOwnershipType())) {
            existing.setOwnershipType(incoming.getOwnershipType());
        }
        if (incoming.getDeclarationConfirmed() != null) {
            existing.setDeclarationConfirmed(incoming.getDeclarationConfirmed());
        }
        if (incoming.getAdditionalDetails() != null) {
            existing.setAdditionalDetails(incoming.getAdditionalDetails());
        }
        if (StringUtils.isNotBlank(incoming.getStatus())) {
            existing.setStatus(incoming.getStatus().trim().toUpperCase());
        }
        existing.setLastModifiedBy(actorUuid);

        // Merge rural detail properties
        RuralPropertyDetail incomingRural = incoming.getRuralPropertyDetail();
        RuralPropertyDetail existingRural = existing.getRuralPropertyDetail();
        if (existingRural == null) {
            existingRural = new RuralPropertyDetail();
            existingRural.setPropertyRecordId(existing.getId());
            existingRural.setId(existing.getId());
            existingRural.setTenantId(existing.getTenantId());
            existing.setRuralPropertyDetail(existingRural);
        }

        if (incomingRural != null) {
            if (StringUtils.isNotBlank(incomingRural.getGramPanchayat())) {
                existingRural.setGramPanchayat(incomingRural.getGramPanchayat());
            }
            if (StringUtils.isNotBlank(incomingRural.getVillageName())) {
                existingRural.setVillageName(incomingRural.getVillageName());
            }
            if (StringUtils.isNotBlank(incomingRural.getWardNumber())) {
                existingRural.setWardNumber(incomingRural.getWardNumber());
            }
            if (StringUtils.isNotBlank(incomingRural.getPropertyAddress())) {
                existingRural.setPropertyAddress(incomingRural.getPropertyAddress());
            }
            if (StringUtils.isNotBlank(incomingRural.getPropertyType())) {
                existingRural.setPropertyType(incomingRural.getPropertyType());
            }
            if (StringUtils.isNotBlank(incomingRural.getPropertyUsage())) {
                existingRural.setPropertyUsage(incomingRural.getPropertyUsage());
            }
            if (StringUtils.isNotBlank(incomingRural.getSurveyNumber())) {
                existingRural.setSurveyNumber(incomingRural.getSurveyNumber());
            }
        }
        existingRural.setLastModifiedBy(actorUuid);

        // Update document proof if new document attached
        if (incoming.getDocument() != null && StringUtils.isNotBlank(incoming.getDocument().getFilestoreId())) {
            User existingCitizen = validateCitizenExists(userUuid);
            UserDocument doc = incoming.getDocument();
            doc.setUserId(existingCitizen.getId());
            doc.setUserUuid(existingCitizen.getUuid());
            doc.setTenantId(existing.getTenantId());
            doc.setReferenceType(UserConstants.DOC_REF_TYPE_PROPERTY);
            doc.setReferenceId(existing.getId());
            doc.setCreatedBy(actorUuid);
            doc.setLastModifiedBy(actorUuid);
            documentRepository.create(doc);
            existing.setDocumentId(doc.getId());
        }

        propertyRepository.saveProperty(existing);
        propertyRepository.saveRuralProperty(existingRural);

        // Update or add co-owners if provided
        if (incoming.getOwners() != null && !incoming.getOwners().isEmpty()) {
            for (PropertyOwner owner : incoming.getOwners()) {
                if (owner == null) continue;
                if (StringUtils.isBlank(owner.getId())) {
                    owner.setId(UUID.randomUUID().toString());
                }
                owner.setPropertyId(existing.getId());
                owner.setTenantId(existing.getTenantId());
                owner.setCreatedBy(actorUuid);
                owner.setLastModifiedBy(actorUuid);
                propertyRepository.savePropertyOwner(owner);
            }
        }

        log.info("[RURAL-PROP-SERVICE] Updated rural property ID: {} for citizen UUID: {}", propertyId, userUuid);
        return propertyRepository.findRuralPropertyById(propertyId);
    }

    /**
     * Searches rural property assets matching the specified criteria.
     *
     * @param criteria    query criteria filters
     * @param requestInfo standard UPYOG request context envelope
     * @return list of matching {@link UserProperty} entities
     */
    @Override
    public List<UserProperty> searchRuralProperties(RuralPropertySearchCriteria criteria, RequestInfo requestInfo) {
        if (criteria == null) {
            throw new CustomException(UserConstants.ERROR_EMPTY_SEARCH_CRITERIA, "Search criteria payload cannot be null");
        }

        RuralPropertySearchCriteria effectiveCriteria = criteria.resolveCriteria();
        if (!effectiveCriteria.hasAnyFilter()) {
            throw new CustomException(UserConstants.ERROR_INVALID_SEARCH_CRITERIA,
                    "At least one search filter (userUuid, propertyId, tenantId, or surveyNumber) must be provided");
        }

        log.info("[RURAL-PROP-SERVICE] Searching rural properties: userUuid={}, propertyId={}, tenantId={}, status={}",
                effectiveCriteria.getUserUuid(), effectiveCriteria.getPropertyId(),
                effectiveCriteria.getTenantId(), effectiveCriteria.getStatus());

        return propertyRepository.searchRuralProperties(effectiveCriteria);
    }

    /**
     * Deactivates / soft-deletes a rural property by setting its status to {@code INACTIVE}.
     *
     * @param request rural property delete/deactivation request payload
     * @return deactivated {@link UserProperty} entity with {@code INACTIVE} status
     */
    @Override
    @Transactional
    public UserProperty deleteRuralProperty(RuralPropertyRequest request) {
        if (request == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_PROPERTY_REQUEST, "Request payload cannot be null");
        }
        String propertyId = request.resolvePropertyId();
        String userUuid = request.resolveUserUuid();
        return deactivateRuralProperty(propertyId, userUuid, request.getRequestInfo());
    }

    /**
     * Direct deactivation helper method marking a rural property as {@code INACTIVE}.
     *
     * @param propertyId  property record primary key ID
     * @param userUuid    citizen user UUID
     * @param requestInfo standard UPYOG request context envelope
     * @return deactivated {@link UserProperty} entity
     */
    @Override
    @Transactional
    public UserProperty deactivateRuralProperty(String propertyId, String userUuid, RequestInfo requestInfo) {
        if (StringUtils.isBlank(propertyId)) {
            throw new CustomException(UserConstants.ERROR_INVALID_PROPERTY_REQUEST, "Property ID is mandatory for deactivation");
        }
        if (StringUtils.isBlank(userUuid)) {
            throw new CustomException(UserConstants.ERROR_USER_UUID_REQUIRED, UserConstants.MSG_CITIZEN_USER_UUID_REQUIRED);
        }

        UserProperty existing = propertyRepository.findRuralPropertyById(propertyId);
        if (existing == null) {
            throw new CustomException(UserConstants.ERROR_PROPERTY_NOT_FOUND, UserConstants.MSG_PROPERTY_NOT_FOUND + propertyId);
        }

        // Security boundary
        if (!userUuid.equals(existing.getUserUuid())) {
            log.warn("[RURAL-PROP-SERVICE] Blocked deactivation attempt: property {} owned by {} requested by {}",
                    propertyId, existing.getUserUuid(), userUuid);
            throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_ACCESS, UserConstants.MSG_PROPERTY_ACCESS_DENIED);
        }

        String actorUuid = resolveActorUuid(requestInfo);
        boolean success = propertyRepository.updatePropertyStatus(propertyId, userUuid, UserConstants.STATUS_INACTIVE_UPPERCASE, actorUuid);
        if (!success) {
            throw new CustomException(UserConstants.ERROR_PROPERTY_NOT_FOUND, "Failed to mark property as inactive: " + propertyId);
        }

        log.info("[RURAL-PROP-SERVICE] Soft-deactivated rural property ID: {} for citizen UUID: {}", propertyId, userUuid);
        return propertyRepository.findRuralPropertyById(propertyId);
    }

    /**
     * Validates that the citizen exists in {@code eg_user}.
     *
     * @param userUuid citizen user UUID
     * @return existing {@link User} entity
     */
    private User validateCitizenExists(String userUuid) {
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .uuid(Collections.singletonList(userUuid))
                .excludeAddressDetails(true)
                .build();
        List<User> found = userRepository.findAll(criteria);
        if (CollectionUtils.isEmpty(found)) {
            throw new CustomException(UserConstants.ERROR_USER_NOT_FOUND,
                    UserConstants.MSG_USER_RECORD_NOT_FOUND_PREFIX + userUuid + UserConstants.MSG_USER_RECORD_NOT_FOUND_SUFFIX);
        }
        return found.get(0);
    }

    /**
     * Resolves audit actor UUID strictly from {@link RequestInfo#getUserInfo()}.
     * Guarantees that mutations to citizen rural property declarations are non-repudiable and cannot
     * be executed anonymously or disguised under generic system actor credentials.
     *
     * @param requestInfo standard UPYOG request context envelope
     * @return validated non-blank actor UUID string
     * @throws CustomException if {@code requestInfo}, {@code userInfo}, or {@code uuid} is missing
     */
    private String resolveActorUuid(RequestInfo requestInfo) {
        if (requestInfo == null || requestInfo.getUserInfo() == null || StringUtils.isBlank(requestInfo.getUserInfo().getUuid())) {
            log.error("[RURAL-PROP-SERVICE] Unauthorized mutation attempt: RequestInfo.userInfo is missing or blank");
            throw new CustomException(UserConstants.ERROR_CALLER_INFO_REQUIRED, UserConstants.MSG_CALLER_INFO_REQUIRED);
        }
        return requestInfo.getUserInfo().getUuid().trim();
    }
}
