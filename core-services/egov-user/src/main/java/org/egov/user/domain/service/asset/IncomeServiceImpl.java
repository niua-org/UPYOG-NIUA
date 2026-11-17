package org.egov.user.domain.service.asset;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserDocument;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.asset.UserIncome;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.UserDocumentRepository;
import org.egov.user.persistence.repository.UserIncomeRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.asset.IncomeRequest;
import org.egov.user.web.contract.asset.IncomeSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service implementation managing standalone CRUD operations and lifecycle status mutations for citizen income declarations.
 *
 * <p>Enforces:</p>
 * <ul>
 *   <li>Citizen existence validation in core {@code eg_user} registry.</li>
 *   <li>Mandatory occupation, annual income, identifier type, and tenantId requirements.</li>
 *   <li>Ownership security boundaries preventing cross-citizen income updates or deletions.</li>
 *   <li>Soft-deactivation setting {@code status = INACTIVE} with complete audit trail persistence.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class IncomeServiceImpl implements IncomeService {

    private static final Set<String> DEFAULT_IDENTIFIER_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("PAN", "TAN", "GSTIN"))
    );

    private final Map<String, CacheEntry> identifierTypeCache = new ConcurrentHashMap<>();

    private final UserIncomeRepository incomeRepository;
    private final UserRepository userRepository;
    private final UserDocumentRepository documentRepository;
    private final AssetAuthorizationService assetAuthorizationService;
    private final UserConfiguration userConfiguration;
    private final RestTemplate restTemplate;

    /**
     * Constructs {@link IncomeServiceImpl} with required persistence repositories, authorization service,
     * and optional MDMS configuration and HTTP client.
     *
     * @param incomeRepository          persistence repository for income declarations
     * @param userRepository            repository for querying citizen user existence
     * @param documentRepository        repository for storing attached salary and tax proof documents
     * @param assetAuthorizationService centralized caller authorization and family-scoping service
     * @param userConfiguration         centralized application and MDMS configurations
     * @param restTemplate              HTTP client for querying external MDMS service
     */
    @Autowired
    public IncomeServiceImpl(UserIncomeRepository incomeRepository,
                             UserRepository userRepository,
                             UserDocumentRepository documentRepository,
                             AssetAuthorizationService assetAuthorizationService,
                             @Autowired(required = false) UserConfiguration userConfiguration,
                             @Autowired(required = false) RestTemplate restTemplate) {
        this.incomeRepository = incomeRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.assetAuthorizationService = assetAuthorizationService != null
                ? assetAuthorizationService
                : new AssetAuthorizationService(userRepository, null);
        this.userConfiguration = userConfiguration;
        this.restTemplate = restTemplate;
    }

    public IncomeServiceImpl(UserIncomeRepository incomeRepository,
                             UserRepository userRepository,
                             UserDocumentRepository documentRepository,
                             AssetAuthorizationService assetAuthorizationService) {
        this(incomeRepository, userRepository, documentRepository, assetAuthorizationService, null, null);
    }

    /**
     * Testing constructor.
     */
    public IncomeServiceImpl(UserIncomeRepository incomeRepository,
                             UserRepository userRepository,
                             UserDocumentRepository documentRepository) {
        this(incomeRepository, userRepository, documentRepository, new AssetAuthorizationService(userRepository, null));
    }

    /**
     * Creates a new citizen income declaration.
     *
     * @param request income creation request payload
     * @return persisted {@link UserIncome} domain entity with populated IDs and audit timestamps
     */
    @Override
    @Transactional
    public UserIncome createIncome(IncomeRequest request) {
        if (request == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_INCOME_REQUEST, "Request payload cannot be null");
        }

        String callerUuid = assetAuthorizationService.resolveCallerUuid(request.getRequestInfo());
        UserIncome inc = request.resolveIncome();
        String userUuid = inc.getUserUuid();
        if (StringUtils.isBlank(userUuid)) {
            if (assetAuthorizationService.isPrivilegedCaller(request.getRequestInfo())) {
                throw new CustomException(UserConstants.ERROR_USER_UUID_REQUIRED, UserConstants.MSG_CITIZEN_USER_UUID_REQUIRED);
            }
            userUuid = callerUuid;
            inc.setUserUuid(userUuid);
        }

        // Enforce caller authorization: caller must be self, Head of Family for member, or privileged employee
        assetAuthorizationService.validateAssetAccess(callerUuid, userUuid, inc.getTenantId(), request.getRequestInfo());

        // Validate citizen exists in eg_user
        User existingCitizen = validateCitizenExists(userUuid);

        // Validate mandatory tenantId
        if (StringUtils.isBlank(inc.getTenantId())) {
            throw new CustomException("INVALID_TENANT_ID", "tenantId is mandatory for income declarations");
        }

        // Validate mandatory income fields
        if (StringUtils.isBlank(inc.getOccupation()) || inc.getAnnualIncome() == null
                || StringUtils.isBlank(inc.getIdentifierType())) {
            throw new CustomException(UserConstants.ERROR_INCOME_MANDATORY_FIELDS, UserConstants.MSG_INCOME_MANDATORY_FIELDS);
        }

        // Validate annual income figure
        if (inc.getAnnualIncome().compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(UserConstants.ERROR_INVALID_INCOME_REQUEST, "Annual income cannot be negative");
        }

        // Validate identifier type against allowable fiscal types
        Set<String> allowedIdentifierTypes = getAllowedIdentifierTypes(request.getRequestInfo(), inc.getTenantId());
        String identifierType = inc.getIdentifierType().trim().toUpperCase();
        if (!allowedIdentifierTypes.contains(identifierType)) {
            throw new CustomException(UserConstants.ERROR_INVALID_INCOME_REQUEST,
                    "Invalid identifierType: " + identifierType + ". Allowable types are " + String.join(", ", allowedIdentifierTypes));
        }
        inc.setIdentifierType(identifierType);

        String actorUuid = callerUuid;

        if (StringUtils.isBlank(inc.getId())) {
            String existingActiveId = incomeRepository.findActiveIncomeIdByUserUuid(userUuid);
            if (existingActiveId != null) {
                inc.setId(existingActiveId);
            } else {
                inc.setId(UUID.randomUUID().toString());
            }
        }

        inc.setUserUuid(userUuid);
        if (StringUtils.isBlank(inc.getStatus())) {
            inc.setStatus(UserConstants.STATUS_ACTIVE_UPPERCASE);
        }
        inc.setCreatedBy(actorUuid);
        inc.setLastModifiedBy(actorUuid);

        // Attach salary proof document if supplied
        if (inc.getSalaryProofDocument() != null && StringUtils.isNotBlank(inc.getSalaryProofDocument().getFilestoreId())) {
            UserDocument doc = inc.getSalaryProofDocument();
            doc.setUserId(existingCitizen.getId());
            doc.setUserUuid(existingCitizen.getUuid());
            doc.setTenantId(inc.getTenantId());
            doc.setReferenceType(UserConstants.DOC_REF_TYPE_INCOME_SALARY);
            doc.setReferenceId(inc.getId());
            doc.setCreatedBy(actorUuid);
            doc.setLastModifiedBy(actorUuid);
            documentRepository.create(doc);
            inc.setSalaryProofDocId(doc.getId());
        }

        // Attach tax proof document if supplied
        if (inc.getTaxProofDocument() != null && StringUtils.isNotBlank(inc.getTaxProofDocument().getFilestoreId())) {
            UserDocument doc = inc.getTaxProofDocument();
            doc.setUserId(existingCitizen.getId());
            doc.setUserUuid(existingCitizen.getUuid());
            doc.setTenantId(inc.getTenantId());
            doc.setReferenceType(UserConstants.DOC_REF_TYPE_INCOME_TAX);
            doc.setReferenceId(inc.getId());
            doc.setCreatedBy(actorUuid);
            doc.setLastModifiedBy(actorUuid);
            documentRepository.create(doc);
            inc.setTaxProofDocId(doc.getId());
        }

        incomeRepository.saveIncome(inc);
        log.info("[INCOME-SERVICE] Created income declaration ID: {} for citizen UUID: {}", inc.getId(), userUuid);
        return incomeRepository.findIncomeById(inc.getId());
    }

    /**
     * Updates an existing citizen income declaration.
     *
     * @param request income update request payload
     * @return updated {@link UserIncome} domain entity
     */
    @Override
    @Transactional
    public UserIncome updateIncome(IncomeRequest request) {
        if (request == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_INCOME_REQUEST, "Request payload cannot be null");
        }

        String callerUuid = assetAuthorizationService.resolveCallerUuid(request.getRequestInfo());
        String incomeId = request.resolveIncomeId();
        if (StringUtils.isBlank(incomeId)) {
            throw new CustomException(UserConstants.ERROR_INVALID_INCOME_REQUEST, "Income ID is mandatory for update");
        }

        UserIncome existing = incomeRepository.findIncomeById(incomeId);
        if (existing == null) {
            throw new CustomException(UserConstants.ERROR_INCOME_NOT_FOUND, UserConstants.MSG_INCOME_NOT_FOUND + incomeId);
        }

        // Enforce caller authorization: caller must be owner, Head of Family for member, or privileged employee
        assetAuthorizationService.validateAssetAccess(callerUuid, existing.getUserUuid(), existing.getTenantId(), request.getRequestInfo());

        // Prevent cross-user ownership reassignment in update payload
        String requestedUserUuid = request.resolveUserUuid();
        if (StringUtils.isNotBlank(requestedUserUuid) && !requestedUserUuid.trim().equalsIgnoreCase(existing.getUserUuid())) {
            log.warn("[INCOME-SERVICE] Ownership reassignment blocked: income {} belongs to {} but update payload contained userUuid {}",
                    incomeId, existing.getUserUuid(), requestedUserUuid);
            throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_ACCESS, UserConstants.MSG_INCOME_ACCESS_DENIED);
        }

        String actorUuid = callerUuid;
        UserIncome incoming = request.resolveIncome();

        if (StringUtils.isNotBlank(incoming.getOccupation())) {
            existing.setOccupation(incoming.getOccupation().trim());
        }

        if (incoming.getAnnualIncome() != null) {
            if (incoming.getAnnualIncome().compareTo(BigDecimal.ZERO) < 0) {
                throw new CustomException(UserConstants.ERROR_INVALID_INCOME_REQUEST, "Annual income cannot be negative");
            }
            existing.setAnnualIncome(incoming.getAnnualIncome());
        }

        if (StringUtils.isNotBlank(incoming.getIdentifierType())) {
            Set<String> allowedIdentifierTypes = getAllowedIdentifierTypes(request.getRequestInfo(), existing.getTenantId());
            String idType = incoming.getIdentifierType().trim().toUpperCase();
            if (!allowedIdentifierTypes.contains(idType)) {
                throw new CustomException(UserConstants.ERROR_INVALID_INCOME_REQUEST,
                        "Invalid identifierType: " + idType + ". Allowable types are " + String.join(", ", allowedIdentifierTypes));
            }
            existing.setIdentifierType(idType);
        }

        if (StringUtils.isNotBlank(incoming.getIdentifierValue())) {
            existing.setIdentifierValue(incoming.getIdentifierValue().trim().toUpperCase());
        }

        if (incoming.getIsVerified() != null) {
            existing.setIsVerified(incoming.getIsVerified());
        }

        if (StringUtils.isNotBlank(incoming.getVerificationSource())) {
            existing.setVerificationSource(incoming.getVerificationSource().trim());
        }

        if (incoming.getVerificationDate() != null) {
            existing.setVerificationDate(incoming.getVerificationDate());
        }

        if (incoming.getOtpVerified() != null) {
            existing.setOtpVerified(incoming.getOtpVerified());
        }

        if (StringUtils.isNotBlank(incoming.getStatus())) {
            existing.setStatus(incoming.getStatus().trim().toUpperCase());
        }

        if (incoming.getAdditionalDetails() != null) {
            existing.setAdditionalDetails(incoming.getAdditionalDetails());
        }

        existing.setLastModifiedBy(actorUuid);

        // Update salary proof document if provided
        if (incoming.getSalaryProofDocument() != null && StringUtils.isNotBlank(incoming.getSalaryProofDocument().getFilestoreId())) {
            User existingCitizen = validateCitizenExists(existing.getUserUuid());
            UserDocument doc = incoming.getSalaryProofDocument();
            doc.setUserId(existingCitizen.getId());
            doc.setUserUuid(existingCitizen.getUuid());
            doc.setTenantId(existing.getTenantId());
            doc.setReferenceType(UserConstants.DOC_REF_TYPE_INCOME_SALARY);
            doc.setReferenceId(existing.getId());
            doc.setCreatedBy(actorUuid);
            doc.setLastModifiedBy(actorUuid);
            documentRepository.create(doc);
            existing.setSalaryProofDocId(doc.getId());
        }

        // Update tax proof document if provided
        if (incoming.getTaxProofDocument() != null && StringUtils.isNotBlank(incoming.getTaxProofDocument().getFilestoreId())) {
            User existingCitizen = validateCitizenExists(existing.getUserUuid());
            UserDocument doc = incoming.getTaxProofDocument();
            doc.setUserId(existingCitizen.getId());
            doc.setUserUuid(existingCitizen.getUuid());
            doc.setTenantId(existing.getTenantId());
            doc.setReferenceType(UserConstants.DOC_REF_TYPE_INCOME_TAX);
            doc.setReferenceId(existing.getId());
            doc.setCreatedBy(actorUuid);
            doc.setLastModifiedBy(actorUuid);
            documentRepository.create(doc);
            existing.setTaxProofDocId(doc.getId());
        }

        incomeRepository.saveIncome(existing);
        log.info("[INCOME-SERVICE] Updated income declaration ID: {} for citizen UUID: {}", incomeId, existing.getUserUuid());
        return incomeRepository.findIncomeById(incomeId);
    }

    /**
     * Searches citizen income declarations matching the specified criteria.
     * Enforces caller authorization and scopes unconstrained citizen searches to their own records.
     *
     * @param criteria    query criteria filters
     * @param requestInfo standard UPYOG request context envelope
     * @return list of matching {@link UserIncome} entities
     */
    @Override
    public List<UserIncome> searchIncome(IncomeSearchCriteria criteria, RequestInfo requestInfo) {
        if (criteria == null) {
            throw new CustomException(UserConstants.ERROR_EMPTY_SEARCH_CRITERIA, "Search criteria payload cannot be null");
        }

        if (requestInfo == null && criteria.getRequestInfo() != null) {
            requestInfo = criteria.getRequestInfo();
        }

        String callerUuid = assetAuthorizationService.resolveCallerUuid(requestInfo);
        IncomeSearchCriteria effectiveCriteria = criteria.resolveCriteria();

        // Enforce caller authorization and auto-scope unconstrained citizen searches
        assetAuthorizationService.scopeSearchCriteria(
                callerUuid,
                effectiveCriteria.getUserUuid(),
                effectiveCriteria.getUserUuids(),
                effectiveCriteria.getTenantId(),
                requestInfo,
                effectiveCriteria::setUserUuid
        );

        // Security Guard: Citizen callers must be scoped to an authorized userUuid
        if (!assetAuthorizationService.isPrivilegedCaller(requestInfo)) {
            if (StringUtils.isBlank(effectiveCriteria.getUserUuid())
                    && (effectiveCriteria.getUserUuids() == null || effectiveCriteria.getUserUuids().isEmpty())) {
                log.warn("[INCOME-SERVICE] Unscoped income search attempted by citizen caller {}", callerUuid);
                throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_ACCESS,
                        "Citizen searches must be scoped to an authorized userUuid");
            }
        }

        if (!effectiveCriteria.hasAnyFilter()) {
            throw new CustomException(UserConstants.ERROR_INVALID_SEARCH_CRITERIA,
                    "At least one search filter (userUuid, incomeId, tenantId, occupation, identifierType, or identifierValue) must be provided");
        }

        log.info("[INCOME-SERVICE] Searching income declarations: userUuid={}, incomeId={}, tenantId={}, status={}",
                effectiveCriteria.getUserUuid(), effectiveCriteria.resolveIncomeId(),
                effectiveCriteria.getTenantId(), effectiveCriteria.getStatus());

        return incomeRepository.searchIncome(effectiveCriteria);
    }

    /**
     * Deactivates / soft-deletes a citizen income declaration by setting its status to {@code INACTIVE}.
     *
     * @param request income delete/deactivation request payload
     * @return deactivated {@link UserIncome} entity with {@code INACTIVE} status
     */
    @Override
    @Transactional
    public UserIncome deleteIncome(IncomeRequest request) {
        if (request == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_INCOME_REQUEST, "Request payload cannot be null");
        }
        String incomeId = request.resolveIncomeId();
        String userUuid = request.resolveUserUuid();
        return deactivateIncome(incomeId, userUuid, request.getRequestInfo());
    }

    /**
     * Direct deactivation helper method marking an income declaration as {@code INACTIVE}.
     * Enforces that the caller is the income declaration owner, Head of Family, or a privileged employee.
     *
     * @param incomeId    income record primary key ID
     * @param userUuid    citizen user UUID
     * @param requestInfo standard UPYOG request context envelope
     * @return deactivated {@link UserIncome} entity
     */
    @Override
    @Transactional
    public UserIncome deactivateIncome(String incomeId, String userUuid, RequestInfo requestInfo) {
        if (StringUtils.isBlank(incomeId)) {
            throw new CustomException(UserConstants.ERROR_INVALID_INCOME_REQUEST, "Income ID is mandatory for deactivation");
        }

        String callerUuid = assetAuthorizationService.resolveCallerUuid(requestInfo);

        UserIncome existing = incomeRepository.findIncomeById(incomeId);
        if (existing == null) {
            throw new CustomException(UserConstants.ERROR_INCOME_NOT_FOUND, UserConstants.MSG_INCOME_NOT_FOUND + incomeId);
        }

        // Enforce caller authorization: caller must be owner, Head of Family for member, or privileged employee
        assetAuthorizationService.validateAssetAccess(callerUuid, existing.getUserUuid(), existing.getTenantId(), requestInfo);

        if (StringUtils.isNotBlank(userUuid) && !userUuid.trim().equalsIgnoreCase(existing.getUserUuid())) {
            log.warn("[INCOME-SERVICE] Blocked deactivation attempt: income {} owned by {} requested with userUuid {}",
                    incomeId, existing.getUserUuid(), userUuid);
            throw new CustomException(UserConstants.ERROR_UNAUTHORIZED_ACCESS, UserConstants.MSG_INCOME_ACCESS_DENIED);
        }

        String actorUuid = callerUuid;
        boolean success = incomeRepository.updateIncomeStatus(incomeId, existing.getUserUuid(), UserConstants.STATUS_INACTIVE_UPPERCASE, actorUuid);
        if (!success) {
            throw new CustomException(UserConstants.ERROR_INCOME_NOT_FOUND, "Failed to mark income declaration as inactive: " + incomeId);
        }

        log.info("[INCOME-SERVICE] Soft-deactivated income declaration ID: {} for citizen UUID: {}", incomeId, existing.getUserUuid());
        return incomeRepository.findIncomeById(incomeId);
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
     * Retrieves allowed income identifier types for the specified tenant, resolving from MDMS
     * when enabled, with automatic fallback to {@code application.properties} and in-memory TTL caching.
     *
     * @param requestInfo caller RequestInfo
     * @param tenantId    tenant identifier
     * @return set of valid uppercase identifier types (e.g. PAN, TAN, GSTIN)
     */
    public Set<String> getAllowedIdentifierTypes(RequestInfo requestInfo, String tenantId) {
        String stateTenant = getStateLevelTenant(tenantId);
        long ttlMillis = (userConfiguration != null && userConfiguration.getIncomeIdentifierTypeCacheTtlMillis() != null)
                ? userConfiguration.getIncomeIdentifierTypeCacheTtlMillis()
                : 300000L;

        CacheEntry entry = identifierTypeCache.get(stateTenant);
        if (entry != null && !entry.isExpired(ttlMillis)) {
            return entry.getTypes();
        }

        Set<String> allowedTypes = new HashSet<>();
        boolean mdmsEnabled = userConfiguration != null && Boolean.TRUE.equals(userConfiguration.getMdmsIncomeIdentifierTypeEnabled());
        String mdmsHost = userConfiguration != null ? userConfiguration.getMdmsHost() : null;
        String mdmsPath = userConfiguration != null ? userConfiguration.getMdmsPath() : null;

        if (mdmsEnabled && restTemplate != null && StringUtils.isNotBlank(mdmsHost) && StringUtils.isNotBlank(mdmsPath)) {
            try {
                allowedTypes = fetchIdentifierTypesFromMdms(requestInfo, stateTenant, mdmsHost, mdmsPath);
                if (allowedTypes != null && !allowedTypes.isEmpty()) {
                    log.info("[INCOME-MDMS] Successfully loaded {} identifier types from MDMS for tenant {}: {}",
                            allowedTypes.size(), stateTenant, allowedTypes);
                    CacheEntry newEntry = new CacheEntry(allowedTypes, System.currentTimeMillis());
                    identifierTypeCache.put(stateTenant, newEntry);
                    return newEntry.getTypes();
                }
            } catch (Exception e) {
                log.warn("[INCOME-MDMS] Failed to fetch IdentifierType from MDMS ({}). Falling back to application.properties: {}",
                        e.getClass().getSimpleName(), e.getMessage());
            }
        }

        // Fallback to application.properties
        allowedTypes = parsePropertiesAllowedTypes();
        log.info("[INCOME-MDMS] Loaded {} default identifier types from application.properties: {}",
                allowedTypes.size(), allowedTypes);
        CacheEntry newEntry = new CacheEntry(allowedTypes, System.currentTimeMillis());
        identifierTypeCache.put(stateTenant, newEntry);
        return newEntry.getTypes();
    }

    private Set<String> fetchIdentifierTypesFromMdms(RequestInfo requestInfo, String stateTenant, String mdmsHost, String mdmsPath) {
        String moduleName = (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getMdmsIncomeIdentifierTypeModuleName()))
                ? userConfiguration.getMdmsIncomeIdentifierTypeModuleName()
                : "common-masters";
        String masterName = (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getMdmsIncomeIdentifierTypeMasterName()))
                ? userConfiguration.getMdmsIncomeIdentifierTypeMasterName()
                : "IncomeIdentifier";
        String filter = (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getMdmsIncomeIdentifierTypeFilter()))
                ? userConfiguration.getMdmsIncomeIdentifierTypeFilter()
                : "[?(@.active==true)]";

        String url = mdmsHost.endsWith("/")
                ? mdmsHost + mdmsPath
                : mdmsHost + "/" + mdmsPath;

        MasterDetail masterDetail = MasterDetail.builder()
                .name(masterName)
                .filter(filter)
                .build();

        ModuleDetail moduleDetail = ModuleDetail.builder()
                .moduleName(moduleName)
                .masterDetails(Collections.singletonList(masterDetail))
                .build();

        MdmsCriteria criteria = new MdmsCriteria();
        criteria.setTenantId(stateTenant);
        criteria.setModuleDetails(Collections.singletonList(moduleDetail));

        MdmsCriteriaReq criteriaReq = new MdmsCriteriaReq();
        criteriaReq.setRequestInfo(requestInfo != null ? requestInfo : new RequestInfo());
        criteriaReq.setMdmsCriteria(criteria);

        JsonNode responseNode = restTemplate.postForObject(url, criteriaReq, JsonNode.class);

        Set<String> types = new HashSet<>();
        if (responseNode != null) {
            JsonNode mdmsRes = responseNode.path("MdmsRes").path(moduleName).path(masterName);
            if (mdmsRes.isMissingNode() || mdmsRes.isNull() || !mdmsRes.isArray()) {
                String altName = "IncomeIdentifier".equalsIgnoreCase(masterName) ? "IncomeIdentifierType" : "IncomeIdentifier";
                mdmsRes = responseNode.path("MdmsRes").path(moduleName).path(altName);
            }
            if (mdmsRes != null && mdmsRes.isArray()) {
                for (JsonNode item : mdmsRes) {
                    if (item.hasNonNull("code")) {
                        types.add(item.get("code").asText().trim().toUpperCase());
                    }
                }
            }
        }
        return types;
    }

    private Set<String> parsePropertiesAllowedTypes() {
        if (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getAllowedIncomeIdentifierTypes())) {
            return Arrays.stream(userConfiguration.getAllowedIncomeIdentifierTypes().split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
        }
        return DEFAULT_IDENTIFIER_TYPES;
    }

    private String getStateLevelTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            return "pg";
        }
        return tenantId.split("\\.")[0];
    }

    private static class CacheEntry {
        private final Set<String> types;
        private final long timestamp;

        public CacheEntry(Set<String> types, long timestamp) {
            this.types = types;
            this.timestamp = timestamp;
        }

        public boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - timestamp > ttlMillis;
        }

        public Set<String> getTypes() {
            return types;
        }
    }
}
