package org.egov.user.domain.service.asset;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.FamilyRestRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Centralized authorization component securing citizen asset CRUD operations (Rural Property, Vehicle, Income).
 *
 * <p>Enforces:</p>
 * <ul>
 *   <li>Non-repudiable caller identification from {@link RequestInfo#getUserInfo()}.</li>
 *   <li>Strict citizen isolation: Citizen callers can only access their own assets or those of household
 *       members where the caller is verified as the Head of Family (HoF) in {@code upyog-family-service}.</li>
 *   <li>Privileged employee and internal system roles can manage assets across citizens.</li>
 *   <li>Search scope enforcement preventing unauthorized data harvesting.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class AssetAuthorizationService {

    private final UserRepository userRepository;
    private final FamilyRestRepository familyRestRepository;
    private final UserConfiguration configuration;

    /**
     * Constructs {@link AssetAuthorizationService} with persistence repositories and configuration.
     *
     * @param userRepository       core user repository
     * @param familyRestRepository REST client for upyog-family-service household queries (optional)
     * @param configuration        centralized application configuration for role bindings
     */
    @Autowired
    public AssetAuthorizationService(UserRepository userRepository,
                                     @Autowired(required = false) FamilyRestRepository familyRestRepository,
                                     UserConfiguration configuration) {
        this.userRepository = userRepository;
        this.familyRestRepository = familyRestRepository;
        this.configuration = configuration;
    }

    /**
     * Convenience constructor for backward-compatible instantiation without explicit configuration.
     *
     * @param userRepository       core user repository
     * @param familyRestRepository REST client for upyog-family-service household queries (optional)
     */
    public AssetAuthorizationService(UserRepository userRepository,
                                     FamilyRestRepository familyRestRepository) {
        this(userRepository, familyRestRepository, null);
    }

    /**
     * Resolves the authenticated caller's UUID strictly from {@link RequestInfo#getUserInfo()}.
     *
     * @param requestInfo request context containing caller credentials
     * @return non-blank caller user UUID string
     * @throws CustomException if caller context is missing or blank
     */
    public String resolveCallerUuid(RequestInfo requestInfo) {
        if (requestInfo == null || requestInfo.getUserInfo() == null || StringUtils.isBlank(requestInfo.getUserInfo().getUuid())) {
            log.error("[ASSET-AUTH] Unauthorized access attempt: RequestInfo.userInfo.uuid is missing or blank");
            throw new CustomException(UserConstants.ERROR_CALLER_INFO_REQUIRED, UserConstants.MSG_CALLER_INFO_REQUIRED);
        }
        return requestInfo.getUserInfo().getUuid().trim();
    }

    /**
     * Determines whether the caller has a privileged administrative, employee, or system role.
     * Privileged role codes are dynamically resolved from {@code egov.asset.privileged.roles} in {@code application.properties}.
     *
     * @param requestInfo request context
     * @return true if caller has privileged role, false if caller is a standard citizen
     */
    public boolean isPrivilegedCaller(RequestInfo requestInfo) {
        if (requestInfo == null || requestInfo.getUserInfo() == null) {
            return false;
        }

        org.egov.common.contract.request.User callerInfo = requestInfo.getUserInfo();
        String userType = callerInfo.getType();

        if (UserType.EMPLOYEE.name().equalsIgnoreCase(userType) || UserType.SYSTEM.name().equalsIgnoreCase(userType)) {
            return true;
        }

        Set<String> privilegedRoles = (configuration != null && configuration.getAssetPrivilegedRoles() != null)
                ? configuration.getAssetPrivilegedRoles()
                : Collections.emptySet();

        if (callerInfo.getRoles() != null) {
            for (Role role : callerInfo.getRoles()) {
                if (role != null && StringUtils.isNotBlank(role.getCode())) {
                    if (privilegedRoles.contains(role.getCode().trim().toUpperCase())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Validates that the caller is authorized to view or mutate assets belonging to {@code targetUserUuid}.
     *
     * @param callerUuid     UUID of the calling actor
     * @param targetUserUuid UUID of the citizen owning the asset
     * @param tenantId       tenant identifier
     * @param requestInfo    request context
     * @throws CustomException with UNAUTHORIZED_ACCESS if caller is unauthorized
     */
    public void validateAssetAccess(String callerUuid, String targetUserUuid, String tenantId, RequestInfo requestInfo) {
        if (StringUtils.isBlank(targetUserUuid)) {
            throw new CustomException(UserConstants.ERROR_USER_UUID_REQUIRED, UserConstants.MSG_CITIZEN_USER_UUID_REQUIRED);
        }

        // 1. Self-ownership: Citizen caller owns the asset
        if (callerUuid.equalsIgnoreCase(targetUserUuid.trim())) {
            return;
        }

        // 2. Privileged access: Employee, System, or Admin role
        if (isPrivilegedCaller(requestInfo)) {
            log.info("[ASSET-AUTH] Privileged actor {} granted access to assets of citizen {}", callerUuid, targetUserUuid);
            return;
        }

        // 3. Head of Family (HoF) delegation: Citizen caller is HoF for the target member
        if (familyRestRepository != null && familyRestRepository.isCallerHeadOfFamilyForMember(callerUuid, targetUserUuid, tenantId, requestInfo)) {
            log.info("[ASSET-AUTH] Head of Family {} authorized to manage assets for family member {}", callerUuid, targetUserUuid);
            return;
        }

        log.warn("[ASSET-AUTH] Unauthorized cross-citizen asset operation blocked: caller {} attempted to access assets of citizen {}",
                callerUuid, targetUserUuid);
        throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_ACCESS,
                "Unauthorized access: You are only permitted to manage your own assets or family member assets as Head of Family.");
    }

    /**
     * Determines whether the caller context is authorized to view or manage assets belonging to targetUserUuid.
     *
     * <p>Permitted when:</p>
     * <ul>
     *   <li>Caller has a privileged employee, administrative, or system role.</li>
     *   <li>Caller is the citizen owning the asset (self-access).</li>
     *   <li>Caller is the active Head of Family (HoF) for the household containing targetUserUuid.</li>
     * </ul>
     *
     * @param targetUserUuid UUID of the citizen owning the asset
     * @param tenantId       tenant identifier
     * @param requestInfo    request context containing caller credentials
     * @return true if caller is authorized to view assets, false otherwise
     */
    public boolean isAuthorizedToViewAssets(String targetUserUuid, String tenantId, RequestInfo requestInfo) {
        if (StringUtils.isBlank(targetUserUuid)) {
            return false;
        }

        // 1. Privileged caller (Employee, System, Admin)
        if (isPrivilegedCaller(requestInfo)) {
            return true;
        }

        // 2. Caller credentials required for citizen access
        if (requestInfo == null || requestInfo.getUserInfo() == null || StringUtils.isBlank(requestInfo.getUserInfo().getUuid())) {
            return false;
        }

        String callerUuid = requestInfo.getUserInfo().getUuid().trim();

        // 3. Self-access
        if (callerUuid.equalsIgnoreCase(targetUserUuid.trim())) {
            return true;
        }

        // 4. Head of Family (HoF) delegation for household member
        if (familyRestRepository != null && familyRestRepository.isCallerHeadOfFamilyForMember(callerUuid, targetUserUuid, tenantId, requestInfo)) {
            log.info("[ASSET-AUTH] Head of Family {} authorized to view assets for family member {}", callerUuid, targetUserUuid);
            return true;
        }

        return false;
    }

    /**
     * Scopes search criteria and validates read access for citizen callers.
     *
     * @param callerUuid       caller's user UUID
     * @param targetUserUuid   explicit userUuid filter in criteria (may be null/blank)
     * @param targetUserUuids  explicit userUuids list in criteria (may be null/empty)
     * @param tenantId         tenant identifier
     * @param requestInfo      request context
     * @param userUuidSetter   callback to set userUuid on criteria when auto-scoping
     */
    public void scopeSearchCriteria(String callerUuid,
                                    String targetUserUuid,
                                    List<String> targetUserUuids,
                                    String tenantId,
                                    RequestInfo requestInfo,
                                    Consumer<String> userUuidSetter) {
        if (isPrivilegedCaller(requestInfo)) {
            // Employees may search across all citizens
            return;
        }

        // Citizen caller: If no userUuid or userUuids is provided, auto-scope to caller's own UUID
        if (StringUtils.isBlank(targetUserUuid) && CollectionUtils.isEmpty(targetUserUuids)) {
            if (userUuidSetter != null) {
                userUuidSetter.accept(callerUuid);
                log.info("[ASSET-AUTH] Auto-scoped search criteria to caller citizen UUID: {}", callerUuid);
            }
            return;
        }

        // If targetUserUuid was specified, validate permission
        if (StringUtils.isNotBlank(targetUserUuid)) {
            validateAssetAccess(callerUuid, targetUserUuid, tenantId, requestInfo);
        }

        // If targetUserUuids list was specified, validate every UUID in the list
        if (!CollectionUtils.isEmpty(targetUserUuids)) {
            for (String uuid : targetUserUuids) {
                if (StringUtils.isNotBlank(uuid)) {
                    validateAssetAccess(callerUuid, uuid, tenantId, requestInfo);
                }
            }
        }
    }
}
