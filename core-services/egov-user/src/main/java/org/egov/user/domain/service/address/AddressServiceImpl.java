package org.egov.user.domain.service.address;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.AddressSearchCriteria;
import org.egov.user.domain.model.enums.AddressType;
import org.egov.user.domain.service.utils.EncryptionDecryptionUtil;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.repository.AddressRepository;
import org.egov.user.persistence.repository.FileStoreRepository;
import org.egov.user.persistence.repository.UserRepository;
import org.egov.user.web.contract.AddressRequest;
import org.egov.user.config.UserConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link AddressService} managing citizen address lifecycle with soft-versioning.
 */
@Service
@Slf4j
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final EncryptionDecryptionUtil encryptionDecryptionUtil;
    private final FileStoreRepository fileStoreRepository;
    private final AddressMasterDataService addressMasterDataService;
    private final UserConfiguration userConfiguration;

    /**
     * Primary constructor injecting all repositories, utilities, and master data services.
     *
     * @param addressRepository        repository for address persistence
     * @param userRepository           repository for user lookups
     * @param encryptionDecryptionUtil utility for address PII encryption/decryption
     * @param fileStoreRepository      repository for downloading address proof documents
     * @param addressMasterDataService service for validating allowed address types from MDMS
     * @param userConfiguration        application configuration properties
     */
    @Autowired
    public AddressServiceImpl(AddressRepository addressRepository,
                              UserRepository userRepository,
                              EncryptionDecryptionUtil encryptionDecryptionUtil,
                              @Autowired(required = false) FileStoreRepository fileStoreRepository,
                              @Autowired(required = false) AddressMasterDataService addressMasterDataService,
                              @Autowired(required = false) UserConfiguration userConfiguration) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.encryptionDecryptionUtil = encryptionDecryptionUtil;
        this.fileStoreRepository = fileStoreRepository;
        this.addressMasterDataService = addressMasterDataService;
        this.userConfiguration = userConfiguration;
    }

    /**
     * Constructor for testing scenarios with address master data service.
     *
     * @param addressRepository        repository for address persistence
     * @param userRepository           repository for user lookups
     * @param encryptionDecryptionUtil utility for address PII encryption/decryption
     * @param fileStoreRepository      repository for downloading address proof documents
     * @param addressMasterDataService service for validating allowed address types from MDMS
     */
    public AddressServiceImpl(AddressRepository addressRepository,
                              UserRepository userRepository,
                              EncryptionDecryptionUtil encryptionDecryptionUtil,
                              FileStoreRepository fileStoreRepository,
                              AddressMasterDataService addressMasterDataService) {
        this(addressRepository, userRepository, encryptionDecryptionUtil, fileStoreRepository, addressMasterDataService, null);
    }

    /**
     * Minimal constructor for legacy unit tests.
     *
     * @param addressRepository        repository for address persistence
     * @param userRepository           repository for user lookups
     * @param encryptionDecryptionUtil utility for address PII encryption/decryption
     * @param fileStoreRepository      repository for downloading address proof documents
     */
    public AddressServiceImpl(AddressRepository addressRepository,
                              UserRepository userRepository,
                              EncryptionDecryptionUtil encryptionDecryptionUtil,
                              FileStoreRepository fileStoreRepository) {
        this(addressRepository, userRepository, encryptionDecryptionUtil, fileStoreRepository, null, null);
    }

    @Override
    public Address createAddress(AddressRequest request) {
        if (request == null || request.getAddress() == null) {
            log.error("[ADDRESS-SERVICE] Address creation failed: request or address is null");
            throw new CustomException(UserConstants.ERROR_INVALID_ADDRESS_REQUEST, UserConstants.MSG_ADDRESS_REQUEST_NULL);
        }

        Address address = request.getAddress();
        Long userId = resolveUserId(request);
        String resolvedUuid = resolveUserUuid(request);
        if (StringUtils.isBlank(resolvedUuid)) {
            resolvedUuid = userRepository.getUuidByUserId(userId);
        }

        String userTenant = userRepository.getTenantIdByUserId(userId);
        String tenantId = resolveTenantId(address.getTenantId(), userTenant);
        address.setTenantId(tenantId);

        if (StringUtils.isNotBlank(address.getStatus())) {
            address.setStatus(address.getStatus().trim().toLowerCase());
        } else {
            address.setStatus(UserConstants.ADDRESS_ACTIVE_STATUS);
        }

        if (StringUtils.isBlank(address.getAddress())) {
            address.setAddress(buildCombinedAddress(address));
        }

        // Validate Section 8 mandatory address fields
        address.validateMandatoryAddressFields();

        // Validate Address Type against UPYOG MDMS / application.properties master data
        if (addressMasterDataService != null) {
            String typeToValidate = StringUtils.isNotBlank(address.getAddressType())
                    ? address.getAddressType()
                    : (address.getType() != null ? address.getType().name() : null);
            addressMasterDataService.validateAddressType(request.getRequestInfo(), tenantId, typeToValidate);
        }

        // Validate attached address proof filestoreId if provided
        validateFileStoreId(tenantId, address.getFilestoreId());

        // 1. Fetch existing active addresses for this user to check for duplicates
        AddressSearchCriteria activeCriteria = AddressSearchCriteria.builder()
                .userId(userId)
                .status(UserConstants.ADDRESS_ACTIVE_STATUS)
                .build();
        List<Address> existingDbAddresses = addressRepository.getAddressV2(activeCriteria);
        List<Address> decryptedExistingList = decryptAddressList(existingDbAddresses);

        if (!CollectionUtils.isEmpty(decryptedExistingList)) {
            for (Address existing : decryptedExistingList) {
                if (isAddressSame(address, existing)) {
                    log.warn("[ADDRESS-SERVICE] Duplicate address rejected for userUuid: {}, type: {}", resolvedUuid, address.getType());
                    throw new CustomException(UserConstants.ERROR_DUPLICATE_ADDRESS, UserConstants.MSG_DUPLICATE_ADDRESS_EXISTS);
                }
            }
        }

        // 2. For single-instance types (PERMANENT only as per rule: permanent can only be one, others can be multiple)
        if (AddressType.PERMANENT == address.getType()) {
            if (!CollectionUtils.isEmpty(existingDbAddresses)) {
                for (Address existing : existingDbAddresses) {
                    if (existing.getType() == address.getType()) {
                        log.info("[ADDRESS-SERVICE] Active address of type PERMANENT already exists for userUuid: {}. Soft-versioning to new record.",
                                resolvedUuid);
                        addressRepository.updateAddressV2(existing.getId(), userId, UserConstants.ADDRESS_INACTIVE_STATUS);
                    }
                }
            }
        }

        address.setUserUuid(resolvedUuid);
        Address encrypted = encryptAddress(address);
        if (encrypted != null && StringUtils.isBlank(encrypted.getUserUuid())) {
            encrypted.setUserUuid(resolvedUuid);
        }
        Address saved = addressRepository.createAddressV2(encrypted, userId, tenantId);
        saved.setUserUuid(resolvedUuid);
        log.info("[ADDRESS-SERVICE] Successfully persisted address ID: {} for userUuid: {}", saved.getId(), resolvedUuid);

        Address decrypted = decryptAddress(saved);
        if (decrypted != null) {
            decrypted.setUserUuid(resolvedUuid);
        }
        return decrypted;
    }

    @Override
    public Address updateAddress(AddressRequest request) {
        if (request == null || request.getAddress() == null) {
            log.error("[ADDRESS-SERVICE] Address update failed: request or address is null");
            throw new CustomException(UserConstants.ERROR_INVALID_ADDRESS_REQUEST, UserConstants.MSG_ADDRESS_REQUEST_NULL);
        }

        Address address = request.getAddress();
        Long addressId = address.getId() != null
                ? address.getId()
                : (StringUtils.isNotBlank(request.getAddressId()) ? Long.valueOf(request.getAddressId()) : null);

        if (addressId == null) {
            log.error("[ADDRESS-SERVICE] Address update failed: addressId is missing");
            throw new CustomException(UserConstants.ERROR_ADDRESS_ID_REQUIRED, UserConstants.MSG_ADDRESS_ID_REQUIRED_UPDATE);
        }

        AddressSearchCriteria criteria = AddressSearchCriteria.builder()
                .id(addressId)
                .status(UserConstants.ADDRESS_ACTIVE_STATUS)
                .build();

        List<Address> existingList = addressRepository.getAddressV2(criteria);
        if (CollectionUtils.isEmpty(existingList)) {
            log.error("[ADDRESS-SERVICE] Active address not found with ID: {}", addressId);
            throw new CustomException(UserConstants.ERROR_DATA_MISMATCH, "Data mismatch: Active address not found with ID: " + addressId);
        }

        Address existing = existingList.get(0);
        if (address.getUserId() != null && existing.getUserId() != null && !address.getUserId().equals(existing.getUserId())) {
            log.error("[ADDRESS-SERVICE] Data mismatch: provided userId {} does not match address owner userId {}", address.getUserId(), existing.getUserId());
            throw new CustomException(UserConstants.ERROR_DATA_MISMATCH, "Data mismatch: Provided userId " + address.getUserId() + " does not match address record");
        }
        Long userId = existing.getUserId();
        String resolvedUuid = resolveUserUuid(request);
        if (StringUtils.isBlank(resolvedUuid)) {
            resolvedUuid = existing.getUserUuid();
        }
        if (StringUtils.isBlank(resolvedUuid)) {
            resolvedUuid = userRepository.getUuidByUserId(userId);
        }

        String userTenant = userRepository.getTenantIdByUserId(userId);
        String tenantId = StringUtils.isNotBlank(address.getTenantId())
                ? address.getTenantId().trim()
                : resolveTenantId(existing.getTenantId(), userTenant);
        address.setTenantId(tenantId);

        if (address.getType() == null) {
            address.setType(existing.getType());
        }

        if (StringUtils.isNotBlank(address.getStatus())) {
            address.setStatus(address.getStatus().trim().toLowerCase());
        } else {
            address.setStatus(UserConstants.ADDRESS_ACTIVE_STATUS);
        }

        if (StringUtils.isBlank(address.getAddress())) {
            address.setAddress(buildCombinedAddress(address));
        }

        Address existingDecrypted = decryptAddress(existing);
        // Enforce Section 8 non-editable address fields (City, Locality, House Number, Pin Code)
        address.validateNonEditableAddressFields(existingDecrypted);

        // Validate Address Type against UPYOG MDMS / application.properties master data if provided
        if (addressMasterDataService != null && (address.getType() != null || StringUtils.isNotBlank(address.getAddressType()))) {
            String typeToValidate = StringUtils.isNotBlank(address.getAddressType())
                    ? address.getAddressType()
                    : (address.getType() != null ? address.getType().name() : null);
            addressMasterDataService.validateAddressType(request.getRequestInfo(), tenantId, typeToValidate);
        }

        // Validate attached address proof filestoreId if provided
        validateFileStoreId(tenantId, address.getFilestoreId());

        if (isAddressSame(address, existingDecrypted)) {
            log.warn("[ADDRESS-SERVICE] Update address rejected: identical details for userUuid: {}, addressId: {}", resolvedUuid, addressId);
            throw new CustomException(UserConstants.ERROR_DUPLICATE_ADDRESS, UserConstants.MSG_DUPLICATE_ADDRESS_IDENTICAL);
        }

        // SCD Type 2 Soft-versioning:
        // 1. Mark existing address record as inactive
        log.info("[ADDRESS-SERVICE] Soft-versioning existing address ID: {} to inactive for userUuid: {}", addressId, resolvedUuid);
        addressRepository.updateAddressV2(existing.getId(), userId, UserConstants.ADDRESS_INACTIVE_STATUS);

        // 2. Prepare and insert new active address record with incremented version
        Long nextVersion = (existing.getVersion() != null ? existing.getVersion() : 0L) + 1L;
        address.setId(null);
        address.setVersion(nextVersion);
        address.setStatus(UserConstants.ADDRESS_ACTIVE_STATUS);
        address.setUserUuid(resolvedUuid);

        Address encrypted = encryptAddress(address);
        if (encrypted != null && StringUtils.isBlank(encrypted.getUserUuid())) {
            encrypted.setUserUuid(resolvedUuid);
        }
        Address saved = addressRepository.createAddressV2(encrypted, userId, tenantId);
        saved.setUserUuid(resolvedUuid);
        log.info("[ADDRESS-SERVICE] Successfully persisted new address version ID: {} (v{}) for userUuid: {}",
                saved.getId(), nextVersion, resolvedUuid);

        Address decrypted = decryptAddress(saved);
        if (decrypted != null) {
            decrypted.setUserUuid(resolvedUuid);
        }
        return decrypted;
    }

    @Override
    public List<Address> searchAddress(AddressSearchCriteria criteria, RequestInfo requestInfo) {
        if (criteria == null) {
            log.error("[ADDRESS-SERVICE] Address search failed: criteria is null");
            throw new CustomException(UserConstants.ERROR_INVALID_SEARCH_CRITERIA, UserConstants.MSG_ADDRESS_SEARCH_CRITERIA_NULL);
        }

        if (StringUtils.isBlank(criteria.getStatus())) {
            criteria.setStatus(UserConstants.ADDRESS_ACTIVE_STATUS);
        } else {
            criteria.setStatus(criteria.getStatus().toLowerCase());
        }

        if (StringUtils.isNotBlank(criteria.getUserUuid()) && criteria.getUserId() == null) {
            Long userId = userRepository.getUserIdByUuid(criteria.getUserUuid());
            if (userId != null) {
                criteria.setUserId(userId);
            }
        }

        log.info("[ADDRESS-SERVICE] Searching addresses with criteria: userUuid={}, userId={}, tenantId={}, status={}",
                criteria.getUserUuid(), criteria.getUserId(), criteria.getTenantId(), criteria.getStatus());

        List<Address> addresses = addressRepository.getAddressV2(criteria);
        if (CollectionUtils.isEmpty(addresses)) {
            return Collections.emptyList();
        }

        List<Address> decryptedList = decryptAddressList(addresses);
        for (Address addr : decryptedList) {
            if (StringUtils.isBlank(addr.getUserUuid())) {
                if (StringUtils.isNotBlank(criteria.getUserUuid())) {
                    addr.setUserUuid(criteria.getUserUuid());
                } else if (addr.getUserId() != null) {
                    addr.setUserUuid(userRepository.getUuidByUserId(addr.getUserId()));
                }
            }
        }
        return decryptedList;
    }

    @Override
    public Address deleteAddress(AddressRequest request) {
        if (request == null) {
            throw new CustomException(UserConstants.ERROR_INVALID_DELETE_REQUEST, UserConstants.MSG_ADDRESS_DELETE_REQUEST_NULL);
        }

        Long addressId = (request.getAddress() != null && request.getAddress().getId() != null)
                ? request.getAddress().getId()
                : (StringUtils.isNotBlank(request.getAddressId()) ? Long.valueOf(request.getAddressId()) : null);

        if (addressId == null) {
            throw new CustomException(UserConstants.ERROR_ADDRESS_ID_REQUIRED, UserConstants.MSG_ADDRESS_ID_REQUIRED_DELETE);
        }

        AddressSearchCriteria criteria = AddressSearchCriteria.builder()
                .id(addressId)
                .build();

        List<Address> existingList = addressRepository.getAddressV2(criteria);
        if (CollectionUtils.isEmpty(existingList)) {
            throw new CustomException(UserConstants.ERROR_ADDRESS_NOT_FOUND, "Address not found with ID: " + addressId);
        }

        Address target = existingList.get(0);
        Long userId = target.getUserId();
        String resolvedUuid = userRepository.getUuidByUserId(userId);

        log.info("[ADDRESS-SERVICE] Soft-deleting address ID: {} for userUuid: {}", addressId, resolvedUuid);
        addressRepository.updateAddressV2(addressId, userId, UserConstants.ADDRESS_INACTIVE_STATUS);

        target.setAddressType(UserConstants.ADDRESS_INACTIVE_STATUS);
        target.setStatus(UserConstants.ADDRESS_INACTIVE_STATUS);
        target.setUserUuid(resolvedUuid);
        return target;
    }

    /**
     * Resolves the numeric internal user ID from the address request using UUID or direct userId.
     *
     * @param request the incoming address request
     * @return resolved numeric user ID
     * @throws CustomException if user cannot be found
     */
    private Long resolveUserId(AddressRequest request) {
        String userUuid = resolveUserUuid(request);
        if (StringUtils.isNotBlank(userUuid)) {
            Long resolvedId = userRepository.getUserIdByUuid(userUuid);
            if (resolvedId != null) {
                return resolvedId;
            }
            log.error("[ADDRESS-SERVICE] User not found with uuid: {}", userUuid);
            throw new CustomException(UserConstants.ERROR_USER_NOT_FOUND, "No user found with uuid: " + userUuid);
        }
        if (request.getAddress() != null && request.getAddress().getUserId() != null) {
            return request.getAddress().getUserId();
        }
        throw new CustomException(UserConstants.ERROR_USER_UUID_REQUIRED, UserConstants.MSG_ADDRESS_USER_UUID_MANDATORY);
    }

    /**
     * Extracts the citizen UUID from the request envelope or nested address payload.
     *
     * @param request the address request envelope
     * @return UUID string or null if not present
     */
    private String resolveUserUuid(AddressRequest request) {
        if (request == null) return null;
        if (StringUtils.isNotBlank(request.getUuid())) {
            return request.getUuid();
        }
        if (StringUtils.isNotBlank(request.getUserUuid())) {
            return request.getUserUuid();
        }
        if (request.getAddress() != null) {
            if (StringUtils.isNotBlank(request.getAddress().getUuid())) {
                return request.getAddress().getUuid();
            }
            if (StringUtils.isNotBlank(request.getAddress().getUserUuid())) {
                return request.getAddress().getUserUuid();
            }
        }
        return null;
    }

    /**
     * Encrypts sensitive fields in the address object using the configured encryption service.
     *
     * @param address unencrypted address model
     * @return address object with PII fields encrypted
     */
    private Address encryptAddress(Address address) {
        try {
            return encryptionDecryptionUtil.encryptObject(address, UserConstants.USER_ADDRESS_ENCRYPTION_KEY, Address.class);
        } catch (Exception e) {
            log.warn("[ADDRESS-SERVICE] Address encryption skipped in dev mode: {}", e.getMessage());
            return address;
        }
    }

    /**
     * Decrypts encrypted fields of an address model into plain text.
     *
     * @param address encrypted address model
     * @return decrypted address model
     */
    private Address decryptAddress(Address address) {
        if (address == null) return null;
        try {
            return encryptionDecryptionUtil.decryptObject(address, UserConstants.USER_ADDRESS_ENCRYPTION_KEY, Address.class, null);
        } catch (Exception e) {
            log.warn("[ADDRESS-SERVICE] Address decryption fallback returned raw object: {}", e.getMessage());
            return address;
        }
    }

    /**
     * Decrypts a collection of address entities.
     *
     * @param addresses list of encrypted address entities
     * @return list of decrypted address entities
     */
    private List<Address> decryptAddressList(List<Address> addresses) {
        if (CollectionUtils.isEmpty(addresses)) return Collections.emptyList();
        try {
            return encryptionDecryptionUtil.decryptObject(addresses, UserConstants.USER_ADDRESS_ENCRYPTION_KEY, Address.class, null);
        } catch (Exception e) {
            log.warn("[ADDRESS-SERVICE] Address list decryption fallback returned raw list: {}", e.getMessage());
            return addresses;
        }
    }

    /**
     * Concatenates granular address components (house number, house name, street, locality, landmark, city, pincode)
     * into a single comma-separated address line string.
     *
     * @param address address model containing granular components
     * @return combined address line string
     */
    private String buildCombinedAddress(Address address) {
        if (address == null) return null;
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(address.getHouseNumber())) {
            parts.add(address.getHouseNumber().trim());
        }
        if (StringUtils.isNotBlank(address.getHouseName())) {
            parts.add(address.getHouseName().trim());
        }
        if (StringUtils.isNotBlank(address.getStreetName())) {
            parts.add(address.getStreetName().trim());
        }
        if (StringUtils.isNotBlank(address.getLocality())) {
            parts.add(address.getLocality().trim());
        }
        if (StringUtils.isNotBlank(address.getLandmark())) {
            parts.add("Near " + address.getLandmark().trim());
        }
        if (StringUtils.isNotBlank(address.getCity())) {
            parts.add(address.getCity().trim());
        }
        if (StringUtils.isNotBlank(address.getPinCode())) {
            parts.add(address.getPinCode().trim());
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    /**
     * Compares two address models to identify whether they represent the identical physical location.
     *
     * @param a1 first address
     * @param a2 second address
     * @return true if all matching granular/legacy fields are equal ignoring case and whitespace
     */
    private boolean isAddressSame(Address a1, Address a2) {
        if (a1 == null || a2 == null) return false;
        if (a1.getType() != a2.getType()) return false;

        boolean granularFieldsMatch = equalsIgnoreCaseAndTrim(a1.getHouseNumber(), a2.getHouseNumber())
                && equalsIgnoreCaseAndTrim(a1.getHouseName(), a2.getHouseName())
                && equalsIgnoreCaseAndTrim(a1.getStreetName(), a2.getStreetName())
                && equalsIgnoreCaseAndTrim(a1.getAddress2(), a2.getAddress2())
                && equalsIgnoreCaseAndTrim(a1.getLandmark(), a2.getLandmark())
                && equalsIgnoreCaseAndTrim(a1.getLocality(), a2.getLocality())
                && equalsIgnoreCaseAndTrim(a1.getCity(), a2.getCity())
                && equalsIgnoreCaseAndTrim(a1.getPinCode(), a2.getPinCode())
                && equalsIgnoreCaseAndTrim(a1.getTenantId(), a2.getTenantId());

        boolean hasGranular = StringUtils.isNotBlank(a1.getHouseNumber())
                || StringUtils.isNotBlank(a1.getHouseName())
                || StringUtils.isNotBlank(a1.getStreetName())
                || StringUtils.isNotBlank(a1.getLocality());

        if (hasGranular) {
            return granularFieldsMatch;
        }

        return equalsIgnoreCaseAndTrim(a1.getAddress(), a2.getAddress())
                && equalsIgnoreCaseAndTrim(a1.getCity(), a2.getCity())
                && equalsIgnoreCaseAndTrim(a1.getPinCode(), a2.getPinCode())
                && equalsIgnoreCaseAndTrim(a1.getTenantId(), a2.getTenantId());
    }

    /**
     * Null-safe case-insensitive string equality comparison after trimming.
     *
     * @param s1 first string
     * @param s2 second string
     * @return true if both trimmed strings match case-insensitively
     */
    private boolean equalsIgnoreCaseAndTrim(String s1, String s2) {
        String clean1 = StringUtils.trimToEmpty(s1);
        String clean2 = StringUtils.trimToEmpty(s2);
        return clean1.equalsIgnoreCase(clean2);
    }

    /**
     * Validates that the provided filestoreId exists in egov-filestore service for the given tenant.
     *
     * @param tenantId    tenant identifier
     * @param fileStoreId filestore identifier to verify
     * @throws CustomException if filestoreId is verified to not exist
     */
    private void validateFileStoreId(String tenantId, String fileStoreId) {
        if (StringUtils.isBlank(fileStoreId) || fileStoreRepository == null) {
            return;
        }
        if (userConfiguration != null && Boolean.FALSE.equals(userConfiguration.getFilestoreValidationEnabled())) {
            log.info("[ADDRESS-SERVICE] filestore.validation.enabled is false; skipping strict filestore validation for fileStoreId: {}", fileStoreId);
            return;
        }
        try {
            String tenant = resolveTenantId(tenantId, null);
            Map<String, String> urlMap = fileStoreRepository.getUrlByFileStoreId(tenant, Collections.singletonList(fileStoreId));
            if (urlMap == null || !urlMap.containsKey(fileStoreId) || StringUtils.isBlank(urlMap.get(fileStoreId))) {
                log.error("[ADDRESS-SERVICE] filestoreId: {} not found in egov-filestore for tenant: {}", fileStoreId, tenant);
                throw new CustomException(UserConstants.ERROR_FILESTORE_NOT_FOUND, UserConstants.MSG_FILESTORE_NOT_FOUND + fileStoreId);
            }
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            log.warn("[ADDRESS-SERVICE] Could not verify filestoreId {} against egov-filestore ({}: {}). Skipping strict validation in unreachable environment.",
                    fileStoreId, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private String resolveTenantId(String tenantId, String fallbackTenant) {
        if (StringUtils.isNotBlank(tenantId)) {
            return tenantId.trim();
        }
        if (StringUtils.isNotBlank(fallbackTenant)) {
            return fallbackTenant.trim();
        }
        if (userConfiguration != null && StringUtils.isNotBlank(userConfiguration.getStateLevelTenantId())) {
            return userConfiguration.getStateLevelTenantId().trim();
        }
        log.error("[ADDRESS-SERVICE] Tenant ID cannot be determined: state-level tenant ID is not configured");
        throw new CustomException(UserConstants.ERROR_TENANT_ID_REQUIRED, "Tenant ID cannot be determined: state-level tenant ID is not configured");
    }
}
