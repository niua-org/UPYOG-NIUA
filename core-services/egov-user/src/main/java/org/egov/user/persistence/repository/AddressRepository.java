package org.egov.user.persistence.repository;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.AddressSearchCriteria;
import org.egov.user.domain.model.enums.AddressType;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.repository.builder.AddressQueryBuilder;
import org.egov.user.repository.rowmapper.AddressRowMapper;
import org.egov.user.repository.rowmapper.AddressRowMapperV2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repository layer for managing user address entities in the {@code eg_user_address} table.
 * Supports legacy and V2 CRUD operations including search, batch update, and status management.
 */
@Slf4j
@Repository
public class AddressRepository {

    public static final String GET_ADDRESS_BY_USERID = "select * from eg_user_address where userid=:userId and tenantid =:tenantId and status=:status";
    public static final String INSERT_ADDRESS_BYUSERID = "insert into eg_user_address (id,type,address,city,pincode,userid,tenantid,createddate,lastmodifieddate,createdby,lastmodifiedby) "
            + "values(:id,:type,:address,:city,:pincode,:userid,:tenantid,:createddate,:lastmodifieddate,:createdby,:lastmodifiedby)";
    public static final String SELECT_NEXT_SEQUENCE = "select nextval('seq_eg_user_address')";
    public static final String DELETE_ADDRESSES = "delete from eg_user_address where id IN (:id)";
    public static final String DELETE_ADDRESS = "delete from eg_user_address where id=:id";
    public static final String UPDATE_ADDRESS_BYIDAND_TENANTID = "update eg_user_address set address=:address,city=:city,pincode=:pincode,lastmodifiedby=:lastmodifiedby,lastmodifieddate=:lastmodifieddate where userid=:userid and tenantid=:tenantid and type=:type";
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private JdbcTemplate jdbcTemplate;
    private AddressQueryBuilder addressQueryBuilder;

    public AddressRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, JdbcTemplate jdbcTemplate,
                             AddressQueryBuilder addressQueryBuilder) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.addressQueryBuilder =  addressQueryBuilder;
    }

    /**
     * api will create the address for particular userId and tenantId.
     *
     * @param address
     * @param userId
     * @param tenantId
     * @return
     */
    public Address create(Address address, Long userId, String tenantId) {
        Map<String, Object> addressInputs = new HashMap<String, Object>();

        addressInputs.put("id", getNextSequence());
        addressInputs.put("type", address.getType().toString());
        addressInputs.put("address", address.getAddress());
        addressInputs.put("city", address.getCity());
        addressInputs.put("pincode", address.getPinCode());
        addressInputs.put("userid", userId);
        addressInputs.put("tenantid", tenantId);
        addressInputs.put("createddate", new Date());
        addressInputs.put("lastmodifieddate", new Date());
        addressInputs.put("createdby", userId);
        addressInputs.put("lastmodifiedby", address.getUserId());
        String status = StringUtils.isNotBlank(address.getStatus())
                ? address.getStatus().trim().toLowerCase()
                : UserConstants.ADDRESS_ACTIVE_STATUS;
        addressInputs.put("status", status); // Set status to active (always lowercase)
        address.setStatus(status);

        namedParameterJdbcTemplate.update(INSERT_ADDRESS_BYUSERID, addressInputs);
        return address;
    }

    /**
     * api will give the next sequence generator for user-address.
     *
     * @return
     */
    private Long getNextSequence() {
        return jdbcTemplate.queryForObject(SELECT_NEXT_SEQUENCE, Long.class);
    }

    /**
     * api will update the address based on userId and tenantId
     *
     * @param domainAddresses
     * @param userId
     * @param tenantId
     */
    public void update(List<Address> domainAddresses, Long userId, String tenantId) {

        final Map<String, Object> Map = new HashMap<String, Object>();
        Map.put("userId", userId);
        Map.put("tenantId", tenantId);
        Map.put("status", UserConstants.ADDRESS_ACTIVE_STATUS);

        List<Address> entityAddresses = namedParameterJdbcTemplate.query(GET_ADDRESS_BY_USERID, Map,
                new AddressRowMapper());

        if (isEmpty(domainAddresses) && isEmpty(entityAddresses)) {
            return;
        }

        conditionallyDeleteAllAddresses(domainAddresses, entityAddresses);
        deleteRemovedAddresses(domainAddresses, entityAddresses);
        createNewAddresses(domainAddresses, entityAddresses, userId, tenantId);
        updateAddresses(domainAddresses, entityAddresses, userId);
    }

    /**
     * api will fetch the user address by userId And tenantId
     *
     * @param userId
     * @param tenantId
     * @return
     */
    public List<Address> find(Long userId, String tenantId) {

        final Map<String, Object> Map = new HashMap<String, Object>();
        Map.put("userId", userId);
        Map.put("status", UserConstants.ADDRESS_ACTIVE_STATUS);
        StringBuilder query = new StringBuilder("select * from eg_user_address where userid=:userId and status=:status");
        if (StringUtils.isNotBlank(tenantId)) {
            if (tenantId.contains(".")) {
                Map.put("tenantId", tenantId);
                Map.put("stateTenantId", tenantId.split("\\.")[0]);
                query.append(" and (tenantid = :tenantId or tenantid = :stateTenantId)");
            } else {
                Map.put("tenantId", tenantId);
                Map.put("tenantPrefix", tenantId + ".%");
                query.append(" and (tenantid = :tenantId or tenantid like :tenantPrefix)");
            }
        }

        List<Address> addressList = namedParameterJdbcTemplate.query(query.toString(), Map,
                new AddressRowMapper());
        return addressList;
    }

    /**
     * api will update the user addresses.
     *
     * @param domainAddresses
     * @param entityAddresses
     * @param userId
     */
    private void updateAddresses(List<Address> domainAddresses, List<Address> entityAddresses, Long userId) {
        Map<String, Address> typeToEntityAddressMap = toMap(entityAddresses);
        domainAddresses.forEach(address -> updateAddress(typeToEntityAddressMap, address, userId));
    }


    /**
     * update the address for particular userId
     *
     * @param typeToEntityAddressMap
     * @param address
     * @param userId
     */
    private void updateAddress(Map<String, Address> typeToEntityAddressMap, Address address, Long userId) {
        final Address matchingEntityAddress = typeToEntityAddressMap.getOrDefault(address.getType().name(), null);

        if (matchingEntityAddress == null) {
            return;
        }
        Map<String, Object> addressInputs = new HashMap<String, Object>();

        addressInputs.put("address", address.getAddress());
        addressInputs.put("type", address.getType().toString());
        addressInputs.put("city", address.getCity());
        addressInputs.put("pincode", address.getPinCode());
        addressInputs.put("userid", userId);
        addressInputs.put("tenantid", matchingEntityAddress.getTenantId());
        addressInputs.put("lastmodifieddate", new Date());
        addressInputs.put("lastmodifiedby", userId);

        namedParameterJdbcTemplate.update(UPDATE_ADDRESS_BYIDAND_TENANTID, addressInputs);
    }

    private Map<String, Address> toMap(List<Address> entityAddresses) {
        return entityAddresses.stream().collect(Collectors.toMap(Address::getAddressType, address -> address));
    }

    private void conditionallyDeleteAllAddresses(List<Address> domainAddresses, List<Address> entityAddresses) {
        if (domainAddresses == null) {
            deleteEntityAddresses(entityAddresses);
        }
    }

    private void deleteEntityAddresses(List<Address> entityAddresses) {

        List<Long> ids = entityAddresses.parallelStream().map(Address::getId).collect(Collectors.toList());
        final Map<String, Object> adressInputs = new HashMap<String, Object>();
        adressInputs.put("id", ids);
        if (ids != null && !ids.isEmpty()) {
            namedParameterJdbcTemplate.update(DELETE_ADDRESSES, adressInputs);
        }
    }

    private void deleteEntityAddress(Address adress) {

        final Map<String, Object> adressInputs = new HashMap<String, Object>();
        adressInputs.put("id", adress.getId());
        namedParameterJdbcTemplate.update(DELETE_ADDRESS, adressInputs);
    }

    private void deleteRemovedAddresses(List<Address> domainAddresses,
                                        List<Address> entityAddresses) {
        final List<String> addressTypesToRetain = getAddressTypesToRetain(domainAddresses);
        entityAddresses.stream().filter(address -> !addressTypesToRetain.contains(address.getType().name()))
                .forEach(address -> deleteEntityAddress(address));
    }

    private void createNewAddresses(List<Address> domainAddresses, List<Address> entityAddresses, Long userId,
                                    String tenantId) {
        final List<AddressType> addressTypesToCreate = getAddressTypesToCreate(domainAddresses, entityAddresses);
        domainAddresses.stream().filter(address -> addressTypesToCreate.contains(address.getType()))
                .forEach(address -> {
                    String addrTenantId = StringUtils.isNotBlank(address.getTenantId())
                            ? address.getTenantId()
                            : tenantId;
                    create(address, userId, addrTenantId);
                });
    }

    private List<String> getAddressTypesToRetain(List<Address> domainAddresses) {
        return domainAddresses.stream().map(address -> address.getType().name()).collect(Collectors.toList());
    }

    private List<AddressType> getAddressTypesToCreate(List<Address> domainAddresses, List<Address> entityAddresses) {
        final List<String> entityAddressTypes = entityAddresses.stream().map(Address::getAddressType)
                .collect(Collectors.toList());

        return domainAddresses.stream().filter(address -> !entityAddressTypes.contains(address.getType().name()))
                .map(Address::getType).collect(Collectors.toList());
    }

    /**
     * Creates a new address record (v2) for the specified user. The userId is determined by the provided UUID.
     *
     * @param address  the Address object containing the address details to be saved
     * @param userId   the numeric user ID corresponding to the provided UUID
     * @param tenantId the tenant identifier
     * @return the created Address object as returned by the repository after insertion
     * @throws DuplicateKeyException if an address with the same unique constraints already exists
     */
    public Address createAddressV2(Address address, Long userId, String tenantId) {
        Map<String, Object> addressInputs = new HashMap<>();
        Long addressId = getNextSequence();  // Generate the next unique address ID from the sequence
        addressInputs.put("id", addressId);
        addressInputs.put("type", address.getType().toString());
        addressInputs.put("address", address.getAddress());
        addressInputs.put("city", address.getCity());
        addressInputs.put("pincode", address.getPinCode());
        String userUuid = address.getUserUuid();
        if (StringUtils.isBlank(userUuid) && userId != null) {
            try {
                userUuid = jdbcTemplate.queryForObject("SELECT uuid FROM eg_user WHERE id = ?", String.class, userId);
            } catch (Exception e) {
                log.debug("User UUID not resolvable via id: {} ({})", userId, e.getMessage());
            }
        }
        addressInputs.put("userid", userId);
        addressInputs.put("user_uuid", userUuid);
        addressInputs.put("tenantid", tenantId);
        addressInputs.put("createddate", new Date());
        addressInputs.put("lastmodifieddate", new Date());
        addressInputs.put("createdby", userId);
        addressInputs.put("lastmodifiedby", address.getLastModifiedBy() != null ? address.getLastModifiedBy() : userId);
        // New fields for v2 insert: additional address details
        addressInputs.put("address2", address.getAddress2());
        addressInputs.put("houseNumber", address.getHouseNumber());
        addressInputs.put("houseName", address.getHouseName());
        addressInputs.put("streetName", address.getStreetName());
        addressInputs.put("landmark", address.getLandmark());
        addressInputs.put("locality", address.getLocality());
        String status = StringUtils.isNotBlank(address.getStatus())
                ? address.getStatus().trim().toLowerCase()
                : UserConstants.ADDRESS_ACTIVE_STATUS;
        addressInputs.put("status", status); // Always lowercase status
        addressInputs.put("version", address.getVersion() != null ? address.getVersion() : 0L);
        addressInputs.put("latitude", address.getLatitude());
        addressInputs.put("longitude", address.getLongitude());
        addressInputs.put("filestore_id", address.getFilestoreId());

        try {
            String query = AddressQueryBuilder.INSERT_ADDRESS_BYUSERID_V2;
            namedParameterJdbcTemplate.update(query, addressInputs);
        } catch (DuplicateKeyException e) {
            throw new DuplicateKeyException("Address already exists for user with type: " + address.getType());
        } catch (DataIntegrityViolationException e) {
            // Handle foreign key constraint violation specifically
            if (e.getMessage() != null && e.getMessage().contains("eg_user_address_user_fkey")) {
                String identifier = StringUtils.isNotBlank(address.getUserUuid()) ? address.getUserUuid() : String.valueOf(userId);
                throw new IllegalArgumentException("User not found: Cannot add address for non-existent user with UUID '" + identifier + "' and tenantId '" + tenantId + "'");
            }
            throw new RuntimeException("Data integrity violation while creating address: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while creating address: " + e.getMessage(), e);
        }
        address.setUserId(userId);
        address.setUserUuid(userUuid);
        address.setId(addressId);
        address.setTenantId(tenantId);
        address.setStatus(status);
        return address;
    }

    /**
     * Updates the status of an address based on the provided addressId.
     *
     * @param addressId     the unique identifier of the address to be updated
     * @param userId        the ID of the user performing the update (for auditing purposes)
     * @param addressStatus the new status to apply (e.g. active, inactive)
     */
    public void updateAddressV2(Long addressId, Long userId, String addressStatus ) {
        final Map<String, Object> params = new HashMap<>();
        String sanitizedStatus = StringUtils.isNotBlank(addressStatus)
                ? addressStatus.trim().toLowerCase()
                : UserConstants.ADDRESS_ACTIVE_STATUS;
        params.put("addressid", addressId);
        params.put("status", sanitizedStatus);
        params.put("lastmodifieddate", new Date());
        params.put("lastmodifiedby", userId);
        String query = AddressQueryBuilder.UPDATE_ADDRESS_STATUS;
        namedParameterJdbcTemplate.update(query, params);
    }

    /**
     * Updates all details of an existing address in place by its ID.
     *
     * @param address   the {@link Address} containing updated field values
     * @param addressId the ID of the address record to update
     * @param userId    the ID of the user performing the modification
     * @param tenantId  the tenant identifier
     * @return updated {@link Address} instance
     */
    public Address updateAddressDetailsV2(Address address, Long addressId, Long userId, String tenantId) {
        final Map<String, Object> params = new HashMap<>();
        String status = StringUtils.isNotBlank(address.getStatus())
                ? address.getStatus().trim().toLowerCase()
                : UserConstants.ADDRESS_ACTIVE_STATUS;

        params.put("id", addressId);
        params.put("type", address.getType() != null ? address.getType().name() : AddressType.PERMANENT.name());
        params.put("address", address.getAddress());
        params.put("city", address.getCity());
        params.put("pincode", address.getPinCode());
        params.put("tenantid", tenantId);
        params.put("address2", address.getAddress2());
        params.put("houseNumber", address.getHouseNumber());
        params.put("houseName", address.getHouseName());
        params.put("streetName", address.getStreetName());
        params.put("landmark", address.getLandmark());
        params.put("locality", address.getLocality());
        params.put("status", status);
        params.put("latitude", address.getLatitude());
        params.put("longitude", address.getLongitude());
        params.put("filestore_id", address.getFilestoreId());
        params.put("lastmodifieddate", new Date());
        params.put("lastmodifiedby", userId);

        namedParameterJdbcTemplate.update(AddressQueryBuilder.UPDATE_ADDRESS_BY_ID_V2, params);

        address.setId(addressId);
        address.setTenantId(tenantId);
        address.setStatus(status);
        return address;
    }

    /**
     * Searches address records based on {@link AddressSearchCriteria}.
     *
     * @param addressSearchCriteria criteria for filtering addresses
     * @return list of matching {@link Address} records
     */
    public List<Address> getAddressV2(AddressSearchCriteria addressSearchCriteria) {

        final List<Object> preparedStatementValues = new ArrayList<>();
        String queryStr = addressQueryBuilder.getAddressQuery(addressSearchCriteria, preparedStatementValues);
        // Add debug logging for the query AND prepared statement values
        log.debug("Query: {}", queryStr);
        log.debug("Prepared Statement Values: {}", preparedStatementValues);

        List<Address> addresses = jdbcTemplate.query(queryStr, preparedStatementValues.toArray(), new AddressRowMapperV2());

        return addresses;
    }

    /**
     * Updates address records: marks old ones inactive and creates new ones if address details differ.
     *
     * @param addresses List of addresses from payload
     * @param userId User ID
     * @param tenantId Tenant ID
     */
    public void updateExistingAddressesV2(List<Address> addresses, Long userId, String tenantId) {
        if (addresses == null || addresses.isEmpty()) return;

        // Fetch existing addresses from DB for the user
        List<Address> dbAddresses = find(userId, tenantId);
        Map<String, Address> dbAddressMap = dbAddresses.stream()
                .collect(Collectors.toMap(a -> a.getType().name(), a -> a));
        // Iterate through payload addresses and compare with existing ones
        for (Address payloadAddress : addresses) {
            if (payloadAddress == null || payloadAddress.getType() == null) continue;

            String addressType = payloadAddress.getType().name();
            Address dbAddress = dbAddressMap.get(addressType);

            if (dbAddress != null && !isAddressSame(payloadAddress, dbAddress)) {
                // Mark existing address as inactive
                updateAddressV2(dbAddressMap.get(addressType).getId(), userId, UserConstants.ADDRESS_INACTIVE_STATUS);
                log.info("Address of type {} for user {} marked as inactive due to change.", addressType, userId);

                // Insert new address as active
                String addrTenantId = StringUtils.isNotBlank(payloadAddress.getTenantId())
                        ? payloadAddress.getTenantId()
                        : tenantId;
                createAddressV2(payloadAddress, userId, addrTenantId);
            } else if (dbAddress == null) {
                // Address type does not exist in DB — create new one
                String addrTenantId = StringUtils.isNotBlank(payloadAddress.getTenantId())
                        ? payloadAddress.getTenantId()
                        : tenantId;
                createAddressV2(payloadAddress, userId, addrTenantId);
            }
        }
    }

    /**
     * Compares all relevant address fields between incoming payload and existing DB record
     * to determine if any address attributes were modified.
     *
     * @param a1 first address object (e.g. payload address)
     * @param a2 second address object (e.g. database address)
     * @return true if all significant fields match, false otherwise
     */
    private boolean isAddressSame(Address a1, Address a2) {
        if (a1 == null || a2 == null) return false;
        // Compare all address fields to determine if they are the same, if any field differs, return false
        return Objects.equals(a1.getAddress(), a2.getAddress()) &&
                Objects.equals(a1.getAddress2(), a2.getAddress2()) &&
                Objects.equals(a1.getHouseNumber(), a2.getHouseNumber()) &&
                Objects.equals(a1.getHouseName(), a2.getHouseName()) &&
                Objects.equals(a1.getStreetName(), a2.getStreetName()) &&
                Objects.equals(a1.getLandmark(), a2.getLandmark()) &&
                Objects.equals(a1.getLocality(), a2.getLocality()) &&
                Objects.equals(a1.getPinCode(), a2.getPinCode()) &&
                Objects.equals(a1.getCity(), a2.getCity()) &&
                Objects.equals(a1.getType(), a2.getType()) &&
                Objects.equals(a1.getTenantId(), a2.getTenantId());
    }

}
