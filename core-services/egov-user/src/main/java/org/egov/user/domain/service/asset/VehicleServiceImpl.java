package org.egov.user.domain.service.asset;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.asset.UserVehicle;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.persistence.repository.UserVehicleRepository;
import org.egov.user.web.contract.asset.VehicleRequest;
import org.egov.user.web.contract.asset.VehicleSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation managing standalone CRUD operations and lifecycle status mutations for citizen vehicle assets.
 *
 * <p>Enforces:</p>
 * <ul>
 *   <li>Citizen existence validation in core {@code eg_user} registry.</li>
 *   <li>Mandatory vehicle registration number, vehicle type, and tenant identifier requirements.</li>
 *   <li>Ownership security boundaries preventing cross-citizen vehicle updates or deletions.</li>
 *   <li>Soft-deactivation setting {@code status = INACTIVE} with complete audit trail persistence.</li>
 *   <li>Registration Certificate (RC) document persistence and linking.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class VehicleServiceImpl implements VehicleService {

    private final UserVehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final UserDocumentRepository documentRepository;
    private final AssetAuthorizationService assetAuthorizationService;

    /**
     * Constructs {@link VehicleServiceImpl} with required persistence repositories and authorization service.
     *
     * @param vehicleRepository  persistence repository for vehicle declarations
     * @param userRepository     repository for querying citizen user existence
     * @param documentRepository repository for storing attached Registration Certificate documents
     * @param assetAuthorizationService centralized caller authorization and family-scoping service
     */
    @Autowired
    public VehicleServiceImpl(UserVehicleRepository vehicleRepository,
                              UserRepository userRepository,
                              UserDocumentRepository documentRepository,
                              AssetAuthorizationService assetAuthorizationService) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.assetAuthorizationService = assetAuthorizationService != null
                ? assetAuthorizationService
                : new AssetAuthorizationService(userRepository, null);
    }

    /**
     * Testing constructor.
     */
    public VehicleServiceImpl(UserVehicleRepository vehicleRepository,
                              UserRepository userRepository,
                              UserDocumentRepository documentRepository) {
        this(vehicleRepository, userRepository, documentRepository, new AssetAuthorizationService(userRepository, null));
    }

    /**
     * Creates a new vehicle declaration for a citizen.
     *
     * @param request vehicle creation request payload
     * @return persisted {@link UserVehicle} domain entity with populated IDs and audit timestamps
     */
    @Override
    @Transactional
    public UserVehicle createVehicle(VehicleRequest request) {
        if (request == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_VEHICLE_REQUEST, "Request payload cannot be null");
        }

        String callerUuid = assetAuthorizationService.resolveCallerUuid(request.getRequestInfo());
        UserVehicle veh = request.resolveVehicle();
        String userUuid = veh.getUserUuid();
        if (StringUtils.isBlank(userUuid)) {
            if (assetAuthorizationService.isPrivilegedCaller(request.getRequestInfo())) {
                throw new CustomException(UserConstants.ERROR_USER_UUID_REQUIRED, UserConstants.MSG_CITIZEN_USER_UUID_REQUIRED);
            }
            userUuid = callerUuid;
            veh.setUserUuid(userUuid);
        }

        // Validate caller authorization: caller must be self, Head of Family for member, or privileged employee
        assetAuthorizationService.validateAssetAccess(callerUuid, userUuid, veh.getTenantId(), request.getRequestInfo());

        // Validate citizen exists in eg_user
        User existingCitizen = validateCitizenExists(userUuid);

        if (StringUtils.isBlank(veh.getVehicleNumber())
                || StringUtils.isBlank(veh.getVehicleType())
                || StringUtils.isBlank(veh.getTenantId())) {
            throw new CustomException(UserConstants.ERROR_VEHICLE_MANDATORY_FIELDS, UserConstants.MSG_VEHICLE_MANDATORY_FIELDS);
        }

        String actorUuid = callerUuid;

        // Uniqueness validation: Ensure vehicle is not already actively registered to another citizen
        String existingOwnerUuid = vehicleRepository.findActiveOwnerByVehicleNumber(veh.getVehicleNumber());
        if (StringUtils.isNotBlank(existingOwnerUuid) && !existingOwnerUuid.equals(userUuid)) {
            log.error("[VEHICLE-SERVICE] Vehicle number {} is already actively registered to citizen: {}",
                    veh.getVehicleNumber(), existingOwnerUuid);
            throw new CustomException(UserConstants.ERROR_VEHICLE_ALREADY_REGISTERED,
                    UserConstants.MSG_VEHICLE_ALREADY_REGISTERED + veh.getVehicleNumber());
        }

        // Natural key deduplication by vehicle registration number if ID is not explicitly provided
        if (StringUtils.isBlank(veh.getId())) {
            if (StringUtils.isNotBlank(veh.getVehicleNumber())) {
                String existingId = vehicleRepository.findVehicleIdByUserAndNumber(userUuid, veh.getVehicleNumber());
                if (existingId != null) {
                    veh.setId(existingId);
                }
            }
            if (StringUtils.isBlank(veh.getId())) {
                veh.setId(UUID.randomUUID().toString());
            }
        }

        veh.setUserUuid(userUuid);
        if (StringUtils.isBlank(veh.getStatus())) {
            veh.setStatus(UserConstants.STATUS_ACTIVE_UPPERCASE);
        }
        veh.setCreatedBy(actorUuid);
        veh.setLastModifiedBy(actorUuid);

        // Attach Registration Certificate document if supplied
        if (veh.getRcDocument() != null && StringUtils.isNotBlank(veh.getRcDocument().getFilestoreId())) {
            UserDocument rcDoc = veh.getRcDocument();
            rcDoc.setUserId(existingCitizen.getId());
            rcDoc.setUserUuid(existingCitizen.getUuid());
            rcDoc.setTenantId(veh.getTenantId());
            rcDoc.setReferenceType(UserConstants.DOC_REF_TYPE_VEHICLE_RC);
            rcDoc.setReferenceId(veh.getId());
            rcDoc.setCreatedBy(actorUuid);
            rcDoc.setLastModifiedBy(actorUuid);
            documentRepository.create(rcDoc);
            veh.setRcDocId(rcDoc.getId());
        }

        vehicleRepository.saveVehicle(veh);
        log.info("[VEHICLE-SERVICE] Successfully registered vehicle ID: {} for citizen userUuid: {}",
                veh.getId(), userUuid);

        return vehicleRepository.findVehicleById(veh.getId());
    }

    /**
     * Updates an existing vehicle declaration for a citizen.
     *
     * @param request vehicle update request payload
     * @return updated {@link UserVehicle} domain entity
     */
    @Override
    @Transactional
    public UserVehicle updateVehicle(VehicleRequest request) {
        if (request == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_VEHICLE_REQUEST, "Request payload cannot be null");
        }

        String callerUuid = assetAuthorizationService.resolveCallerUuid(request.getRequestInfo());
        String vehicleId = request.resolveVehicleId();
        if (StringUtils.isBlank(vehicleId)) {
            throw new CustomException(UserConstants.ERROR_INVALID_REQUEST, "Vehicle ID is required for update");
        }

        UserVehicle existing = vehicleRepository.findVehicleById(vehicleId);
        if (existing == null) {
            log.error("[VEHICLE-SERVICE] Vehicle not found with ID: {}", vehicleId);
            throw new CustomException(UserConstants.ERROR_VEHICLE_NOT_FOUND, UserConstants.MSG_VEHICLE_NOT_FOUND + vehicleId);
        }

        // Enforce caller authorization: caller must be owner, Head of Family for member, or privileged employee
        assetAuthorizationService.validateAssetAccess(callerUuid, existing.getUserUuid(), existing.getTenantId(), request.getRequestInfo());

        // Prevent cross-user ownership reassignment
        String requestedUserUuid = request.resolveUserUuid();
        if (StringUtils.isNotBlank(requestedUserUuid) && !requestedUserUuid.trim().equalsIgnoreCase(existing.getUserUuid())) {
            log.error("[VEHICLE-SERVICE] Ownership reassignment blocked: vehicle {} belongs to {} but update requested with userUuid {}",
                    vehicleId, existing.getUserUuid(), requestedUserUuid);
            throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_ACCESS, UserConstants.MSG_VEHICLE_ACCESS_DENIED);
        }

        String actorUuid = callerUuid;
        UserVehicle incoming = request.resolveVehicle();

        // Merge attributes
        if (StringUtils.isNotBlank(incoming.getVehicleNumber())
                && !incoming.getVehicleNumber().trim().equalsIgnoreCase(existing.getVehicleNumber())) {
            String incomingNum = incoming.getVehicleNumber().trim();
            String existingOwnerUuid = vehicleRepository.findActiveOwnerByVehicleNumber(incomingNum);
            if (StringUtils.isNotBlank(existingOwnerUuid) && !existingOwnerUuid.equals(existing.getUserUuid())) {
                log.error("[VEHICLE-SERVICE] Cannot update vehicle number to {}: already actively registered to citizen: {}",
                        incomingNum, existingOwnerUuid);
                throw new CustomException(UserConstants.ERROR_VEHICLE_ALREADY_REGISTERED,
                        UserConstants.MSG_VEHICLE_ALREADY_REGISTERED + incomingNum);
            }
            existing.setVehicleNumber(incomingNum);
        }
        if (StringUtils.isNotBlank(incoming.getPurchaseDate())) {
            existing.setPurchaseDate(incoming.getPurchaseDate().trim());
        }
        if (StringUtils.isNotBlank(incoming.getVehicleType())) {
            existing.setVehicleType(incoming.getVehicleType().trim());
        }
        if (StringUtils.isNotBlank(incoming.getMakerModel())) {
            existing.setMakerModel(incoming.getMakerModel().trim());
        }
        if (StringUtils.isNotBlank(incoming.getFuelType())) {
            existing.setFuelType(incoming.getFuelType().trim());
        }
        if (StringUtils.isNotBlank(incoming.getCategory())) {
            existing.setCategory(incoming.getCategory().trim());
        }
        if (StringUtils.isNotBlank(incoming.getStatus())) {
            existing.setStatus(incoming.getStatus().trim().toUpperCase());
        }
        if (incoming.getIsVerified() != null) {
            existing.setIsVerified(incoming.getIsVerified());
        }
        if (StringUtils.isNotBlank(incoming.getValidationSource())) {
            existing.setValidationSource(incoming.getValidationSource().trim());
        }
        if (incoming.getAdditionalDetails() != null) {
            existing.setAdditionalDetails(incoming.getAdditionalDetails());
        }
        existing.setLastModifiedBy(actorUuid);

        // Update Registration Certificate document if new document attached
        if (incoming.getRcDocument() != null && StringUtils.isNotBlank(incoming.getRcDocument().getFilestoreId())) {
            User existingCitizen = validateCitizenExists(existing.getUserUuid());
            UserDocument rcDoc = incoming.getRcDocument();
            rcDoc.setUserId(existingCitizen.getId());
            rcDoc.setUserUuid(existingCitizen.getUuid());
            rcDoc.setTenantId(existing.getTenantId());
            rcDoc.setReferenceType(UserConstants.DOC_REF_TYPE_VEHICLE_RC);
            rcDoc.setReferenceId(existing.getId());
            rcDoc.setCreatedBy(actorUuid);
            rcDoc.setLastModifiedBy(actorUuid);
            documentRepository.create(rcDoc);
            existing.setRcDocId(rcDoc.getId());
            existing.setRcDocument(rcDoc);
        }

        vehicleRepository.saveVehicle(existing);
        log.info("[VEHICLE-SERVICE] Successfully updated vehicle ID: {} for citizen: {}",
                existing.getId(), existing.getUserUuid());

        return vehicleRepository.findVehicleById(existing.getId());
    }

    /**
     * Searches vehicle declarations matching the specified criteria.
     *
     * @param criteria    query criteria filters
     * @param requestInfo standard UPYOG request context envelope
     * @return list of matching {@link UserVehicle} entities
     */
    @Override
    public List<UserVehicle> searchVehicles(VehicleSearchCriteria criteria, RequestInfo requestInfo) {
        if (requestInfo == null && criteria != null && criteria.getRequestInfo() != null) {
            requestInfo = criteria.getRequestInfo();
        }

        String callerUuid = assetAuthorizationService.resolveCallerUuid(requestInfo);

        if (criteria != null && criteria.getCriteria() != null) {
            VehicleSearchCriteria inner = criteria.getCriteria();
            if (inner.getRequestInfo() == null) {
                inner.setRequestInfo(criteria.getRequestInfo());
            }
            if (inner.getLimit() == null) {
                inner.setLimit(criteria.getLimit());
            }
            if (inner.getOffset() == null) {
                inner.setOffset(criteria.getOffset());
            }
            criteria = inner;
        }

        if (criteria == null) {
            criteria = new VehicleSearchCriteria();
        }

        // Enforce caller authorization and auto-scope unconstrained citizen searches
        assetAuthorizationService.scopeSearchCriteria(
                callerUuid,
                criteria.getUserUuid(),
                criteria.getUserUuids(),
                criteria.getTenantId(),
                requestInfo,
                criteria::setUserUuid
        );

        // Security Guard: Citizen callers must be scoped to an authorized userUuid
        if (!assetAuthorizationService.isPrivilegedCaller(requestInfo)) {
            if (StringUtils.isBlank(criteria.getUserUuid())
                    && (criteria.getUserUuids() == null || criteria.getUserUuids().isEmpty())) {
                log.warn("[VEHICLE-SERVICE] Unscoped vehicle search attempted by citizen caller {}", callerUuid);
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_ACCESS,
                        "Citizen searches must be scoped to an authorized userUuid");
            }
        }

        log.debug("[VEHICLE-SERVICE] Searching vehicles with criteria: userUuid={}, vehicleId={}, tenantId={}",
                criteria.getUserUuid(), criteria.getVehicleId(), criteria.getTenantId());

        return vehicleRepository.searchVehicles(criteria);
    }

    /**
     * Deactivates / soft-deletes a vehicle declaration by setting its status to {@code INACTIVE}.
     *
     * @param request vehicle delete/deactivation request payload
     * @return deactivated {@link UserVehicle} entity
     */
    @Override
    @Transactional
    public UserVehicle deleteVehicle(VehicleRequest request) {
        if (request == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_VEHICLE_REQUEST, "Request payload cannot be null");
        }

        String callerUuid = assetAuthorizationService.resolveCallerUuid(request.getRequestInfo());
        String vehicleId = request.resolveVehicleId();
        if (StringUtils.isBlank(vehicleId)) {
            throw new CustomException(UserConstants.ERROR_INVALID_REQUEST, "Vehicle ID is required for deactivation");
        }

        UserVehicle existing = vehicleRepository.findVehicleById(vehicleId);
        if (existing == null) {
            log.error("[VEHICLE-SERVICE] Vehicle not found with ID: {}", vehicleId);
            throw new CustomException(UserConstants.ERROR_VEHICLE_NOT_FOUND, UserConstants.MSG_VEHICLE_NOT_FOUND + vehicleId);
        }

        // Enforce caller authorization: caller must be owner, Head of Family for member, or privileged employee
        assetAuthorizationService.validateAssetAccess(callerUuid, existing.getUserUuid(), existing.getTenantId(), request.getRequestInfo());

        String requestedUserUuid = request.resolveUserUuid();
        if (StringUtils.isNotBlank(requestedUserUuid) && !requestedUserUuid.trim().equalsIgnoreCase(existing.getUserUuid())) {
            log.error("[VEHICLE-SERVICE] Unauthorized deactivation attempt: vehicle {} belongs to {} but deletion requested with userUuid {}",
                    vehicleId, existing.getUserUuid(), requestedUserUuid);
            throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_ACCESS, UserConstants.MSG_VEHICLE_ACCESS_DENIED);
        }

        String actorUuid = callerUuid;
        existing.setStatus(UserConstants.STATUS_INACTIVE_UPPERCASE);
        existing.setLastModifiedBy(actorUuid);

        vehicleRepository.saveVehicle(existing);
        log.info("[VEHICLE-SERVICE] Soft-deactivated vehicle ID: {} for citizen: {}",
                existing.getId(), existing.getUserUuid());

        return existing;
    }

    /**
     * Validates that the target citizen exists in {@code eg_user}.
     *
     * @param userUuid citizen user UUID
     * @return resolved {@link User} entity
     * @throws CustomException if the citizen record does not exist
     */
    private User validateCitizenExists(String userUuid) {
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .uuid(Collections.singletonList(userUuid))
                .build();
        List<User> found = userRepository.findAll(criteria);
        if (CollectionUtils.isEmpty(found)) {
            throw new CustomException(UserConstants.ERROR_USER_NOT_FOUND,
                    UserConstants.MSG_USER_RECORD_NOT_FOUND_PREFIX + userUuid + UserConstants.MSG_USER_RECORD_NOT_FOUND_SUFFIX);
        }
        return found.get(0);
    }
}
